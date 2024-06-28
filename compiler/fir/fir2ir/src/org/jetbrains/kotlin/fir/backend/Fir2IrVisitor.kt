/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.findChildByType
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.generators.ClassMemberGenerator
import org.jetbrains.kotlin.fir.backend.generators.OperatorExpressionGenerator
import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.deserialization.toQualifiedPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.builders.primitiveOp2
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorClassImpl
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultConstructor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.findIsInstanceAnd

class Fir2IrVisitor(
    private val c: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by c, FirDefaultVisitor<IrElement, Any?>() {
    internal val implicitCastInserter = Fir2IrImplicitCastInserter(c)

    private val memberGenerator = ClassMemberGenerator(c, this, conversionScope)

    private val operatorGenerator = OperatorExpressionGenerator(c, this, conversionScope)

    private var _annotationMode: Boolean = false
    val annotationMode: Boolean
        get() = _annotationMode

    internal inline fun <T> withAnnotationMode(enableAnnotationMode: Boolean = true, block: () -> T): T {
        val oldAnnotationMode = _annotationMode
        _annotationMode = enableAnnotationMode
        try {
            return block()
        } finally {
            _annotationMode = oldAnnotationMode
        }
    }

    override fun visitElement(element: FirElement, data: Any?): IrElement {
        error("Should not be here: ${element::class} ${element.render()}")
    }

    override fun visitField(field: FirField, data: Any?): IrField = whileAnalysing(session, field) {
        require(field.isSynthetic) { "Non-synthetic field found during traversal of FIR tree: ${field.render()}" }
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        return declarationStorage.getCachedIrFieldSymbolForSupertypeDelegateField(field)!!.owner.apply {
            // If this is a property backing field, then it has no separate initializer,
            // so we shouldn't convert it
            if (correspondingPropertySymbol == null) {
                memberGenerator.convertFieldContent(this, field)
            }
        }
    }

    override fun visitFile(file: FirFile, data: Any?): IrFile {
        val irFile = declarationStorage.getIrFile(file)
        conversionScope.withParent(irFile) {
            file.declarations.forEach {
                it.toIrDeclaration()
            }
            annotationGenerator.generate(this, file)
            metadata = FirMetadataSource.File(file)
        }
        return irFile
    }

    private fun FirDeclaration.toIrDeclaration(): IrDeclaration =
        accept(this@Fir2IrVisitor, null) as IrDeclaration

    // ==================================================================================

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?): IrElement = whileAnalysing(session, typeAlias) {
        val irTypeAlias = classifierStorage.getCachedTypeAlias(typeAlias)!!
        annotationGenerator.generate(irTypeAlias, typeAlias)
        return irTypeAlias
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?): IrElement = whileAnalysing(session, enumEntry) {
        // At this point all IR for source enum entries should be created and bound to symbols
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val irEnumEntry = classifierStorage.getIrEnumEntrySymbol(enumEntry).owner
        annotationGenerator.generate(irEnumEntry, enumEntry)
        val correspondingClass = irEnumEntry.correspondingClass
        val initializer = enumEntry.initializer
        val irType = enumEntry.returnTypeRef.toIrType(c)
        val irParentEnumClass = irEnumEntry.parent as? IrClass
        // If the enum entry has its own members, we need to introduce a synthetic class.
        if (correspondingClass != null) {
            declarationStorage.enterScope(irEnumEntry.symbol)
            classifierStorage.putEnumEntryClassInScope(enumEntry, correspondingClass)
            val anonymousObject = (enumEntry.initializer as FirAnonymousObjectExpression).anonymousObject
            converter.processAnonymousObjectHeaders(anonymousObject, correspondingClass)
            converter.processClassMembers(anonymousObject, correspondingClass)
            conversionScope.withParent(correspondingClass) {
                memberGenerator.convertClassContent(correspondingClass, anonymousObject)

                // `correspondingClass` definitely is not a lazy class
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                val constructor = correspondingClass.constructors.first()
                irEnumEntry.initializerExpression = irFactory.createExpressionBody(
                    IrEnumConstructorCallImpl(
                        startOffset, endOffset, irType,
                        constructor.symbol,
                        typeArgumentsCount = constructor.typeParameters.size,
                        valueArgumentsCount = constructor.valueParameters.size
                    )
                )
            }
            declarationStorage.leaveScope(irEnumEntry.symbol)
        } else if (initializer is FirAnonymousObjectExpression) {
            // Otherwise, this is a default-ish enum entry, which doesn't need its own synthetic class.
            // During raw FIR building, we put the delegated constructor call inside an anonymous object.
            val delegatedConstructor = initializer.anonymousObject.primaryConstructorIfAny(session)?.fir?.delegatedConstructor
            if (delegatedConstructor != null) {
                with(memberGenerator) {
                    irEnumEntry.initializerExpression = irFactory.createExpressionBody(
                        delegatedConstructor.toIrDelegatingConstructorCall()
                    )
                }
            }
        } else if (irParentEnumClass != null && !irParentEnumClass.isExpect && initializer == null) {
            // a default-ish enum entry whose initializer would be a delegating constructor call
            // `irParentEnumClass` definitely is not a lazy class
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val constructor = irParentEnumClass.defaultConstructor
                ?: error("Assuming that default constructor should exist and be converted at this point")
            enumEntry.convertWithOffsets { startOffset, endOffset ->
                irEnumEntry.initializerExpression = irFactory.createExpressionBody(
                    IrEnumConstructorCallImpl(
                        startOffset, endOffset, irType, constructor.symbol,
                        valueArgumentsCount = constructor.valueParameters.size,
                        typeArgumentsCount = constructor.typeParameters.size
                    )
                )
                irEnumEntry
            }
        }
        return irEnumEntry
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?): IrElement = whileAnalysing(session, regularClass) {
        if (regularClass.visibility == Visibilities.Local) {
            val irParent = conversionScope.parentFromStack()
            // NB: for implicit types it is possible that local class is already cached
            val irClass = classifierStorage.getCachedIrLocalClass(regularClass)?.apply { this.parent = irParent }
            if (irClass != null) {
                conversionScope.withParent(irClass) {
                    memberGenerator.convertClassContent(irClass, regularClass)
                }
                return irClass
            }
            converter.processLocalClassAndNestedClasses(regularClass, irParent)
        }
        val irClass = classifierStorage.getIrClass(regularClass)
        if (regularClass.isSealed) {
            irClass.sealedSubclasses = regularClass.getIrSymbolsForSealedSubclasses(c)
        }
        conversionScope.withParent(irClass) {
            memberGenerator.convertClassContent(irClass, regularClass)
        }
        return irClass
    }

    @OptIn(UnexpandedTypeCheck::class)
    override fun visitScript(script: FirScript, data: Any?): IrElement {
        return declarationStorage.getCachedIrScript(script)!!.also { irScript ->
            irScript.parent = conversionScope.parentFromStack()
            declarationStorage.enterScope(irScript.symbol)

            irScript.explicitCallParameters = script.parameters.map { parameter ->
                declarationStorage.createAndCacheIrVariable(
                    parameter,
                    irScript,
                    givenOrigin = IrDeclarationOrigin.SCRIPT_CALL_PARAMETER
                )
            }

            // NOTE: index should correspond to one generated in the collectTowerDataElementsForScript
            irScript.implicitReceiversParameters = script.receivers.mapIndexedNotNull { index, receiver ->
                val isSelf = receiver.isBaseClassReceiver
                val name =
                    if (isSelf) SpecialNames.THIS
                    else Name.identifier("${SCRIPT_RECEIVER_NAME_PREFIX}_$index")
                val origin = if (isSelf) IrDeclarationOrigin.INSTANCE_RECEIVER else IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER
                val irReceiver =
                    receiver.convertWithOffsets { startOffset, endOffset ->
                        irFactory.createValueParameter(
                            startOffset, endOffset, origin, name, receiver.typeRef.toIrType(c), isAssignable = false,
                            IrValueParameterSymbolImpl(),
                            if (isSelf) UNDEFINED_PARAMETER_INDEX else index,
                            varargElementType = null, isCrossinline = false, isNoinline = false, isHidden = false
                        ).also {
                            it.parent = irScript
                        }
                    }
                if (isSelf) {
                    irScript.thisReceiver = irReceiver
                    irScript.baseClass = irReceiver.type
                    null
                } else irReceiver
            }

            conversionScope.withParent(irScript) {
                val destructComposites = mutableMapOf<FirVariableSymbol<*>, IrComposite>()
                for (statement in script.declarations) {
                    if (statement !is FirAnonymousInitializer) {
                        val irStatement = when {
                            statement is FirProperty && statement.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR -> {
                                continue
                            }
                            statement is FirProperty && statement.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> {
                                // Generating the result property only for expressions with a meaningful result type
                                // otherwise skip the property and convert the expression into the statement
                                if (statement.returnTypeRef.let { (it.isUnit || it.isNothing || it.isNullableNothing) }) {
                                    statement.initializer!!.toIrStatement()
                                } else {
                                    (statement.accept(this@Fir2IrVisitor, null) as? IrDeclaration)?.also {
                                        irScript.resultProperty = (it as? IrProperty)?.symbol
                                    }
                                }
                            }
                            statement is FirVariable && statement.isDestructuringDeclarationContainerVariable == true -> {
                                statement.convertWithOffsets { startOffset, endOffset ->
                                    IrCompositeImpl(
                                        startOffset, endOffset,
                                        builtins.unitType, IrStatementOrigin.DESTRUCTURING_DECLARATION
                                    ).also { composite ->
                                        composite.statements.add(
                                            declarationStorage.createAndCacheIrVariable(statement, conversionScope.parentFromStack()).also {
                                                it.initializer = statement.initializer?.toIrStatement() as? IrExpression
                                            }
                                        )
                                        destructComposites[(statement).symbol] = composite
                                    }
                                }
                            }
                            statement is FirProperty && statement.destructuringDeclarationContainerVariable != null -> {
                                (statement.accept(this@Fir2IrVisitor, null) as IrProperty).also {
                                    val irComponentInitializer = IrSetFieldImpl(
                                        it.startOffset, it.endOffset,
                                        it.backingField!!.symbol,
                                        builtins.unitType,
                                        origin = null, superQualifierSymbol = null
                                    ).apply {
                                        value = it.backingField!!.initializer!!.expression
                                        receiver = null
                                    }
                                    val correspondingComposite = destructComposites[statement.destructuringDeclarationContainerVariable!!]!!
                                    correspondingComposite.statements.add(irComponentInitializer)
                                    it.backingField!!.initializer = null
                                }
                            }
                            statement is FirClass -> {
                                statement.accept(this@Fir2IrVisitor, null) as IrClass
                            }
                            else -> {
                                statement.accept(this@Fir2IrVisitor, null) as? IrDeclaration
                            }
                        }
                        irScript.statements.add(irStatement!!)
                    } else {
                        statement.body?.statements?.mapNotNull { it.toIrStatement() }?.let {
                            irScript.statements.addAll(it)
                        }
                    }
                }
            }
            for (configurator in session.extensionService.fir2IrScriptConfigurators) {
                with(configurator) {
                    irScript.configure(script) { declarationStorage.getCachedIrScript(it.fir)?.symbol }
                }
            }
            declarationStorage.leaveScope(irScript.symbol)
        }
    }

    override fun visitCodeFragment(codeFragment: FirCodeFragment, data: Any?): IrElement {
        val irClass = classifierStorage.getCachedIrCodeFragment(codeFragment)!!
        // class for code fragment definitely is not a lazy class
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val irFunction = irClass.declarations.firstIsInstance<IrSimpleFunction>()

        declarationStorage.enterScope(irFunction.symbol)
        conversionScope.withParent(irFunction) {
            val irBlock = codeFragment.block.convertToIrBlock(forceUnitType = false)
            irFunction.body = irFactory.createExpressionBody(irBlock)
        }
        declarationStorage.leaveScope(irFunction.symbol)

        return irFunction
    }

    override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: Any?): IrElement {
        return visitAnonymousObject(anonymousObjectExpression.anonymousObject, data)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): IrElement = whileAnalysing(
        session, anonymousObject
    ) {
        val irParent = conversionScope.parentFromStack()
        // NB: for implicit types it is possible that anonymous object is already cached
        val irAnonymousObject = classifierStorage.getCachedIrLocalClass(anonymousObject)?.apply { this.parent = irParent }
            ?: converter.processLocalClassAndNestedClasses(anonymousObject, irParent)

        conversionScope.withParent(irAnonymousObject) {
            memberGenerator.convertClassContent(irAnonymousObject, anonymousObject)
        }
        val anonymousClassType = irAnonymousObject.thisReceiver!!.type
        return anonymousObject.convertWithOffsets { startOffset, endOffset ->
            IrBlockImpl(
                startOffset, endOffset, anonymousClassType, IrStatementOrigin.OBJECT_LITERAL,
                listOf(
                    irAnonymousObject,
                    IrConstructorCallImpl.fromSymbolOwner(
                        startOffset,
                        endOffset,
                        anonymousClassType,
                        // a class for an anonymous object definitely is not a lazy class
                        @OptIn(UnsafeDuringIrConstructionAPI::class)
                        irAnonymousObject.constructors.first().symbol,
                        irAnonymousObject.typeParameters.size,
                        origin = IrStatementOrigin.OBJECT_LITERAL
                    )
                )
            )
        }
    }

    // ==================================================================================

    override fun visitConstructor(constructor: FirConstructor, data: Any?): IrElement = whileAnalysing(session, constructor) {
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val irConstructor = declarationStorage.getCachedIrConstructorSymbol(constructor)!!.owner
        return conversionScope.withFunction(irConstructor) {
            memberGenerator.convertFunctionContent(irConstructor, constructor, containingClass = conversionScope.containerFirClass())
        }
    }

    override fun visitAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: Any?
    ): IrElement = whileAnalysing(session, anonymousInitializer) {
        val irAnonymousInitializer = declarationStorage.getIrAnonymousInitializer(anonymousInitializer)
        declarationStorage.enterScope(irAnonymousInitializer.symbol)
        conversionScope.withInitBlock(irAnonymousInitializer) {
            irAnonymousInitializer.body = convertToIrBlockBody(anonymousInitializer.body!!)
        }
        declarationStorage.leaveScope(irAnonymousInitializer.symbol)
        return irAnonymousInitializer
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?): IrElement = whileAnalysing(session, simpleFunction) {
        val irFunction = if (simpleFunction.visibility == Visibilities.Local) {
            declarationStorage.createAndCacheIrFunction(
                simpleFunction, irParent = conversionScope.parent(), predefinedOrigin = IrDeclarationOrigin.LOCAL_FUNCTION, isLocal = true
            )
        } else {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            declarationStorage.getCachedIrFunctionSymbol(simpleFunction)!!.owner
        }
        return conversionScope.withFunction(irFunction) {
            memberGenerator.convertFunctionContent(
                irFunction, simpleFunction, containingClass = conversionScope.containerFirClass()
            )
        }
    }

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: Any?): IrElement {
        return visitAnonymousFunction(anonymousFunctionExpression.anonymousFunction, data)
    }

    override fun visitAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: Any?
    ): IrElement = whileAnalysing(session, anonymousFunction) {
        return anonymousFunction.convertWithOffsets { startOffset, endOffset ->
            val irFunction = declarationStorage.createAndCacheIrFunction(
                anonymousFunction,
                irParent = conversionScope.parent(),
                predefinedOrigin = IrDeclarationOrigin.LOCAL_FUNCTION,
                isLocal = true
            )
            conversionScope.withFunction(irFunction) {
                memberGenerator.convertFunctionContent(irFunction, anonymousFunction, containingClass = null)
            }

            val type = anonymousFunction.typeRef.toIrType(c)

            IrFunctionExpressionImpl(
                startOffset, endOffset, type, irFunction,
                if (irFunction.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) IrStatementOrigin.LAMBDA
                else IrStatementOrigin.ANONYMOUS_FUNCTION
            )
        }
    }

    private fun visitLocalVariable(variable: FirProperty): IrElement = whileAnalysing(session, variable) {
        assert(variable.isLocal)
        val delegate = variable.delegate
        if (delegate != null) {
            val irProperty = declarationStorage.createAndCacheIrLocalDelegatedProperty(variable, conversionScope.parentFromStack())
            irProperty.delegate.initializer = convertToIrExpression(delegate, isDelegate = true)
            conversionScope.withFunction(irProperty.getter) {
                memberGenerator.convertFunctionContent(irProperty.getter, variable.getter, null)
            }
            irProperty.setter?.let {
                conversionScope.withFunction(it) {
                    memberGenerator.convertFunctionContent(it, variable.setter, null)
                }
            }
            return irProperty
        }
        val initializer = variable.initializer
        val isNextVariable = initializer is FirFunctionCall &&
                initializer.calleeReference.toResolvedNamedFunctionSymbol()?.callableId?.isIteratorNext() == true &&
                variable.source?.isChildOfForLoop == true
        val irVariable = declarationStorage.createAndCacheIrVariable(
            variable, conversionScope.parentFromStack(),
            if (isNextVariable) {
                if (variable.name.isSpecial && variable.name == SpecialNames.DESTRUCT) {
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                } else {
                    IrDeclarationOrigin.FOR_LOOP_VARIABLE
                }
            } else {
                null
            }
        )
        if (initializer != null) {
            irVariable.initializer =
                convertToIrExpression(initializer)
                    .insertImplicitCast(initializer, initializer.resolvedType, variable.returnTypeRef.coneType)
        }
        annotationGenerator.generate(irVariable, variable)
        return irVariable
    }

    private fun IrExpression.insertImplicitCast(
        baseExpression: FirExpression,
        valueType: ConeKotlinType,
        expectedType: ConeKotlinType,
    ) =
        with(implicitCastInserter) {
            this@insertImplicitCast.insertSpecialCast(baseExpression, valueType, expectedType)
        }

    override fun visitProperty(property: FirProperty, data: Any?): IrElement = whileAnalysing(session, property) {
        if (property.isLocal) return visitLocalVariable(property)
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val irProperty = declarationStorage.getCachedIrPropertySymbol(property, fakeOverrideOwnerLookupTag = null)?.owner
            ?: return IrErrorExpressionImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT),
                "Stub for Enum.entries"
            )
        return conversionScope.withProperty(irProperty, property) {
            memberGenerator.convertPropertyContent(irProperty, property)
        }
    }

    // ==================================================================================

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Any?): IrElement {
        val result = returnExpression.result
        if (result is FirThrowExpression) {
            // Note: in FIR we must have 'return' as the last statement
            return convertToIrExpression(result)
        }
        val irTarget = conversionScope.returnTarget(returnExpression, declarationStorage)
        return returnExpression.convertWithOffsets { startOffset, endOffset ->
            // For implicit returns, use the expression endOffset to generate the expected line number for debugging.
            val returnStartOffset = if (returnExpression.source?.kind is KtFakeSourceElementKind.ImplicitReturn) endOffset else startOffset
            IrReturnImpl(returnStartOffset, endOffset, builtins.nothingType, irTarget, convertToIrExpression(result))
        }.let {
            returnExpression.accept(implicitCastInserter, it)
        }
    }

    override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: Any?): IrElement {
        // Note: we deal with specific arguments in CallAndReferenceGenerator
        return convertToIrExpression(wrappedArgumentExpression.expression)
    }

    override fun visitSamConversionExpression(samConversionExpression: FirSamConversionExpression, data: Any?): IrElement {
        return convertToIrExpression(samConversionExpression.expression)
    }

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: Any?): IrElement {
        return varargArgumentsExpression.convertWithOffsets { startOffset, endOffset ->
            IrVarargImpl(
                startOffset,
                endOffset,
                varargArgumentsExpression.resolvedType.toIrType(c),
                varargArgumentsExpression.coneElementTypeOrNull?.toIrType(c)
                    ?: error("Vararg expression has incorrect type"),
                varargArgumentsExpression.arguments.map { it.convertToIrVarargElement() }
            )
        }
    }

    private fun FirExpression.convertToIrVarargElement(): IrVarargElement =
        if (this is FirSpreadArgumentExpression) {
            IrSpreadElementImpl(
                source?.startOffset ?: UNDEFINED_OFFSET,
                source?.endOffset ?: UNDEFINED_OFFSET,
                convertToIrExpression(this)
            )
        } else convertToIrExpression(this)

    private fun convertToIrCall(functionCall: FirFunctionCall): IrExpression {
        if (functionCall.isCalleeDynamic &&
            functionCall.calleeReference.name == OperatorNameConventions.SET &&
            functionCall.calleeReference.source?.kind == KtFakeSourceElementKind.ArrayAccessNameReference
        ) {
            return convertToIrArraySetDynamicCall(functionCall)
        }
        return convertToIrCall(functionCall, dynamicOperator = null)
    }

    private fun convertToIrCall(
        functionCall: FirFunctionCall,
        dynamicOperator: IrDynamicOperator?
    ): IrExpression {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            functionCall.explicitReceiver,
            functionCall
        )
        return callGenerator.convertToIrCall(
            functionCall,
            functionCall.resolvedType,
            explicitReceiverExpression,
            dynamicOperator
        )
    }

    private fun convertToIrArraySetDynamicCall(functionCall: FirFunctionCall): IrExpression {
        // `functionCall` has the form of `myDynamic.set(key1, key2, ..., newValue)`.
        // The resulting IR expects something like `myDynamic.ARRAY_ACCESS(key1, key2, ...).EQ(newValue)`.
        // Instead of constructing a `FirFunctionCall` for `get()` (the true `ARRAY_ACCESS`), and a new
        // call for `set()` (`EQ`), let's convert the whole thing as `ARRAY_ACCESS`, including
        // `newValue`, and then manually move it to a newly constructed EQ call.
        val arraySetAsGenericDynamicAccess = convertToIrCall(functionCall, IrDynamicOperator.ARRAY_ACCESS) as? IrDynamicOperatorExpression
            ?: error("Converting dynamic array access should have resulted in IrDynamicOperatorExpression")
        val arraySetNewValue = arraySetAsGenericDynamicAccess.arguments.removeLast()
        return IrDynamicOperatorExpressionImpl(
            arraySetAsGenericDynamicAccess.startOffset,
            arraySetAsGenericDynamicAccess.endOffset,
            arraySetAsGenericDynamicAccess.type,
            IrDynamicOperator.EQ,
        ).apply {
            receiver = arraySetAsGenericDynamicAccess
            arguments.add(arraySetNewValue)
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?): IrExpression = whileAnalysing(session, functionCall) {
        return convertToIrCall(functionCall = functionCall)
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: Any?
    ): IrElement = whileAnalysing(session, safeCallExpression) {
        val explicitReceiverExpression = convertToIrExpression(safeCallExpression.receiver)

        val (receiverVariable, variableSymbol) = conversionScope.createTemporaryVariableForSafeCallConstruction(
            explicitReceiverExpression
        )

        return conversionScope.withSafeCallSubject(receiverVariable) {
            val afterNotNullCheck =
                (safeCallExpression.selector as? FirExpression)?.let(::convertToIrExpression)
                    ?: safeCallExpression.selector.accept(this, data) as IrExpression
            c.createSafeCallConstruction(receiverVariable, variableSymbol, afterNotNullCheck)
        }
    }

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: Any?): IrElement {
        val lastSubjectVariable = conversionScope.lastSafeCallSubject()
        return checkedSafeCallSubject.convertWithOffsets { startOffset, endOffset ->
            IrGetValueImpl(startOffset, endOffset, lastSubjectVariable.type, lastSubjectVariable.symbol)
        }
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: Any?): IrElement {
        return callGenerator.convertToIrConstructorCall(annotation)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): IrElement = whileAnalysing(session, annotationCall) {
        return callGenerator.convertToIrConstructorCall(annotationCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Any?): IrElement {
        return convertQualifiedAccessExpression(qualifiedAccessExpression)
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Any?): IrElement {
        return convertQualifiedAccessExpression(propertyAccessExpression)
    }

    private fun convertQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
    ): IrExpression = whileAnalysing(session, qualifiedAccessExpression) {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            qualifiedAccessExpression.explicitReceiver, qualifiedAccessExpression
        )
        return callGenerator.convertToIrCall(
            qualifiedAccessExpression, qualifiedAccessExpression.resolvedType, explicitReceiverExpression
        )
    }

    // Note that this mimics psi2ir [StatementGenerator#shouldGenerateReceiverAsSingletonReference].
    private fun shouldGenerateReceiverAsSingletonReference(irClassSymbol: IrClassSymbol): Boolean {
        val scopeOwner = conversionScope.parent()
        // For anonymous initializers
        if ((scopeOwner as? IrDeclaration)?.symbol == irClassSymbol) return false
        // Members of object
        return when (scopeOwner) {
            is IrFunction, is IrProperty, is IrField -> {
                val parent = (scopeOwner as IrDeclaration).parent as? IrDeclaration
                parent?.symbol != irClassSymbol
            }
            else -> true
        }
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Any?
    ): IrElement = whileAnalysing(session, thisReceiverExpression) {
        val calleeReference = thisReceiverExpression.calleeReference

        val boundSymbol = calleeReference.boundSymbol

        if (boundSymbol !is FirClassSymbol || calleeReference.contextReceiverNumber == -1) {
            callGenerator.injectGetValueCall(thisReceiverExpression, calleeReference)?.let { return it }
        }

        when (boundSymbol) {
            is FirClassSymbol -> generateThisReceiverAccessForClass(thisReceiverExpression, boundSymbol)
            is FirScriptSymbol -> generateThisReceiverAccessForScript(thisReceiverExpression, boundSymbol)
            is FirCallableSymbol -> generateThisReceiverAccessForCallable(thisReceiverExpression, boundSymbol)
            else -> null
        } ?: visitQualifiedAccessExpression(thisReceiverExpression, data)
    }

    private fun generateThisReceiverAccessForClass(
        thisReceiverExpression: FirThisReceiverExpression,
        firClassSymbol: FirClassSymbol<*>,
    ): IrElement? {
        // Object case
        val calleeReference = thisReceiverExpression.calleeReference
        val firClass = firClassSymbol.fir
        val irClassSymbol = if (firClass.origin.fromSource || firClass.origin.generated) {
            // We anyway can use 'else' branch as fallback, but
            // this is an additional check of FIR2IR invariants
            // (source classes should be already built when we analyze bodies)
            classifierStorage.getIrClass(firClass).symbol
        } else {
            /*
             * The only case when we can refer to non-source this is resolution to companion object of parent
             *   class in some constructor scope:
             *
             * // MODULE: lib
             * abstract class Base {
             *     companion object {
             *         fun foo(): Int = 1
             *     }
             * }
             *
             * // MODULE: app(lib)
             * class Derived(
             *     val x: Int = foo() // this: Base.Companion
             * ) : Base()
             */
            classifierStorage.getIrClassSymbol(firClassSymbol)
        }

        if (firClass.classKind.isObject && shouldGenerateReceiverAsSingletonReference(irClassSymbol)) {
            return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                val irType = firClassSymbol.defaultType().toIrType(c)
                IrGetObjectValueImpl(startOffset, endOffset, irType, irClassSymbol)
            }
        }

        val irClass = conversionScope.findDeclarationInParentsStack<IrClass>(irClassSymbol)

        val dispatchReceiver = conversionScope.dispatchReceiverParameter(irClass) ?: return null
        return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
            val thisRef = callGenerator.findInjectedValue(calleeReference)?.let {
                callGenerator.useInjectedValue(it, calleeReference, startOffset, endOffset)
            } ?: IrGetValueImpl(startOffset, endOffset, dispatchReceiver.type, dispatchReceiver.symbol)

            if (calleeReference.contextReceiverNumber == -1) {
                return thisRef
            }

            val constructorForCurrentlyGeneratedDelegatedConstructor =
                conversionScope.getConstructorForCurrentlyGeneratedDelegatedConstructor(irClass.symbol)

            if (constructorForCurrentlyGeneratedDelegatedConstructor != null) {
                val constructorParameter =
                    constructorForCurrentlyGeneratedDelegatedConstructor.valueParameters[calleeReference.contextReceiverNumber]
                IrGetValueImpl(startOffset, endOffset, constructorParameter.type, constructorParameter.symbol)
            } else {
                val contextReceivers =
                    c.classifierStorage.getFieldsWithContextReceiversForClass(irClass, firClass)
                require(contextReceivers.size > calleeReference.contextReceiverNumber) {
                    "Not defined context receiver #${calleeReference.contextReceiverNumber} for $irClass. " +
                            "Context receivers found: $contextReceivers"
                }

                IrGetFieldImpl(
                    startOffset, endOffset, contextReceivers[calleeReference.contextReceiverNumber].symbol,
                    thisReceiverExpression.resolvedType.toIrType(c),
                    thisRef,
                )
            }
        }
    }

    private fun generateThisReceiverAccessForScript(
        thisReceiverExpression: FirThisReceiverExpression,
        firScriptSymbol: FirScriptSymbol
    ): IrElement {
        val calleeReference = thisReceiverExpression.calleeReference
        val firScript = firScriptSymbol.fir
        val irScript = declarationStorage.getCachedIrScript(firScript) ?: error("IrScript for ${firScript.name} not found")
        val receiverParameter =
            irScript.implicitReceiversParameters.find { it.index == calleeReference.contextReceiverNumber } ?: irScript.thisReceiver
        if (receiverParameter != null) {
            return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                IrGetValueImpl(startOffset, endOffset, receiverParameter.type, receiverParameter.symbol)
            }
        } else {
            error("No script receiver found") // TODO: check if any valid situations possible here
        }
    }

    private fun generateThisReceiverAccessForCallable(
        thisReceiverExpression: FirThisReceiverExpression,
        firCallableSymbol: FirCallableSymbol<*>
    ): IrElement? {
        val calleeReference = thisReceiverExpression.calleeReference
        val irFunction = when (firCallableSymbol) {
            is FirFunctionSymbol -> {
                val functionSymbol = declarationStorage.getIrFunctionSymbol(firCallableSymbol)
                conversionScope.findDeclarationInParentsStack<IrSimpleFunction>(functionSymbol)
            }
            is FirPropertySymbol -> {
                val property = declarationStorage.getIrPropertySymbol(firCallableSymbol) as? IrPropertySymbol
                property?.let { conversionScope.parentAccessorOfPropertyFromStack(it) }
            }
            else -> null
        } ?: return null

        val receiver = if (calleeReference.contextReceiverNumber != -1) {
            irFunction.valueParameters[calleeReference.contextReceiverNumber]
        } else {
            irFunction.extensionReceiverParameter
        } ?: return null

        return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
            IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
        }
    }

    override fun visitInaccessibleReceiverExpression(
        inaccessibleReceiverExpression: FirInaccessibleReceiverExpression,
        data: Any?,
    ): IrElement {
        return inaccessibleReceiverExpression.convertWithOffsets { startOffset, endOffset ->
            IrErrorExpressionImpl(
                startOffset, endOffset,
                inaccessibleReceiverExpression.resolvedType.toIrType(c),
                "Receiver is inaccessible"
            )
        }
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: Any?): IrElement {
        // Generate the expression with the original type and then cast it to the smart cast type.
        val value = convertToIrExpression(smartCastExpression.originalExpression)
        return implicitCastInserter.visitSmartCastExpression(smartCastExpression, value)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Any?): IrElement {
        return whileAnalysing(session, callableReferenceAccess) {
            convertCallableReferenceAccess(callableReferenceAccess, false)
        }
    }

    private fun convertCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, isDelegate: Boolean): IrElement {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            callableReferenceAccess.explicitReceiver, callableReferenceAccess
        )
        return callGenerator.convertToIrCallableReference(
            callableReferenceAccess,
            explicitReceiverExpression,
            isDelegate = isDelegate
        )
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: Any?
    ): IrExpression = whileAnalysing(session, variableAssignment) {
        val explicitReceiverExpression = variableAssignment.explicitReceiver?.let { receiverExpression ->
            convertToIrReceiverExpression(
                receiverExpression, variableAssignment.unwrapLValue()!!
            )
        }
        return callGenerator.convertToIrSetCall(variableAssignment, explicitReceiverExpression)
    }

    override fun visitDesugaredAssignmentValueReferenceExpression(
        desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
        data: Any?
    ): IrElement {
        return desugaredAssignmentValueReferenceExpression.expressionRef.value.accept(this, null)
    }

    override fun visitLiteralExpression(literalExpression: FirLiteralExpression, data: Any?): IrElement {
        return literalExpression.toIrConst<Any?>(literalExpression.resolvedType.toIrType(c))
    }

    // ==================================================================================

    private fun FirStatement.toIrStatement(): IrStatement? {
        if (this is FirTypeAlias) return null
        if (this is FirUnitExpression) return runUnless(source?.kind is KtFakeSourceElementKind.ImplicitUnit.IndexedAssignmentCoercion) {
            convertToIrExpression(this)
        }
        if (this is FirContractCallBlock) return null
        if (this is FirBlock) return convertToIrExpression(this)
        if (this is FirProperty && name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) return null
        return accept(this@Fir2IrVisitor, null) as IrStatement
    }

    internal fun convertToIrExpression(
        expression: FirExpression,
        isDelegate: Boolean = false,
        // This argument is used for a corner case with deserialized empty array literals
        // These array literals normally have a type of Array<Any>,
        // so FIR2IR should instead use a type of corresponding property
        // See also KT-62598
        expectedType: ConeKotlinType? = null,
    ): IrExpression {
        return when (expression) {
            is FirBlock -> {
                val origin = when (expression.source?.kind) {
                    is KtFakeSourceElementKind.DesugaredForLoop -> IrStatementOrigin.FOR_LOOP
                    is KtFakeSourceElementKind.DesugaredAugmentedAssign ->
                        augmentedAssignSourceKindToIrStatementOrigin[expression.source?.kind]
                    is KtFakeSourceElementKind.DesugaredIncrementOrDecrement -> incOrDecSourceKindToIrStatementOrigin[expression.source?.kind]
                    else -> null
                }
                expression.convertToIrExpressionOrBlock(origin)
            }
            is FirUnitExpression -> expression.convertWithOffsets { _, endOffset ->
                IrGetObjectValueImpl(
                    endOffset, endOffset, builtins.unitType, this.builtins.unitClass
                )
            }
            else -> {
                when (val unwrappedExpression = expression.unwrapArgument()) {
                    is FirCallableReferenceAccess -> convertCallableReferenceAccess(unwrappedExpression, isDelegate)
                    is FirArrayLiteral -> convertToArrayLiteral(unwrappedExpression, expectedType)
                    else -> expression.accept(this, null) as IrExpression
                }
            }
        }.let {
            expression.accept(implicitCastInserter, it) as IrExpression
        }
    }

    internal fun convertToIrReceiverExpression(
        receiver: FirExpression?,
        selector: FirQualifiedAccessExpression,
    ): IrExpression? {
        val selectorCalleeReference = selector.calleeReference
        val irReceiver = when (receiver) {
            null -> return null
            is FirResolvedQualifier -> callGenerator.convertToGetObject(receiver, selector as? FirCallableReferenceAccess)
            is FirFunctionCall,
            is FirThisReceiverExpression,
            is FirCallableReferenceAccess,
            is FirSmartCastExpression -> convertToIrExpression(receiver)

            is FirQualifiedAccessExpression -> when (receiver.explicitReceiver) {
                null -> {
                    val variableAsFunctionMode = selectorCalleeReference is FirResolvedNamedReference &&
                            selectorCalleeReference.name != OperatorNameConventions.INVOKE &&
                            (selectorCalleeReference.resolvedSymbol as? FirCallableSymbol)?.callableId?.callableName == OperatorNameConventions.INVOKE
                    callGenerator.convertToIrCall(
                        receiver, receiver.resolvedType, explicitReceiverExpression = null,
                        variableAsFunctionMode = variableAsFunctionMode
                    )
                }
                else -> convertToIrExpression(receiver)
            }
            else -> convertToIrExpression(receiver)
        } ?: return null

        if (receiver is FirQualifiedAccessExpression && receiver.calleeReference is FirSuperReference) return irReceiver

        return implicitCastInserter.implicitCastFromReceivers(
            irReceiver, receiver, selector,
            conversionScope.defaultConversionTypeOrigin()
        )
    }

    private fun List<FirStatement>.mapToIrStatements(recognizePostfixIncDec: Boolean = true): List<IrStatement?> {
        var index = 0
        val result = ArrayList<IrStatement?>(size)
        while (index < size) {
            var irStatement: IrStatement? = null
            if (recognizePostfixIncDec) {
                val statementsOrigin = getStatementsOrigin(index)
                if (statementsOrigin != null) {
                    val subStatements = this.subList(index, index + 3).mapNotNull { it.toIrStatement() }
                    irStatement = IrBlockImpl(
                        subStatements[0].startOffset,
                        subStatements[2].endOffset,
                        (subStatements[0] as IrVariable).type,
                        statementsOrigin,
                        subStatements
                    )
                    index += 3
                }
            }

            if (irStatement == null) {
                irStatement = this[index].toIrStatement()
                index++
            }
            result.add(irStatement)
        }

        return result
    }

    private fun List<FirStatement>.getStatementsOrigin(index: Int): IrStatementOrigin? {
        val incrementStatement = getOrNull(index + 1)
        if (incrementStatement !is FirVariableAssignment) return null

        return incrementStatement.getIrPrefixPostfixOriginIfAny()
    }

    internal fun convertToIrBlockBody(block: FirBlock): IrBlockBody {
        return block.convertWithOffsets { startOffset, endOffset ->
            val irStatements = block.statements.mapToIrStatements()
            irFactory.createBlockBody(
                startOffset, endOffset,
                if (irStatements.isNotEmpty()) {
                    irStatements.filterNotNull().takeIf { it.isNotEmpty() }
                        ?: listOf(IrBlockImpl(startOffset, endOffset, builtins.unitType, null, emptyList()))
                } else {
                    emptyList()
                }
            ).also {
                with(implicitCastInserter) {
                    it.insertImplicitCasts()
                }
            }
        }
    }

    private val IrStatementOrigin.isLoop: Boolean
        get() {
            return this == IrStatementOrigin.DO_WHILE_LOOP || this == IrStatementOrigin.WHILE_LOOP || this == IrStatementOrigin.FOR_LOOP
        }

    private fun extractOperationFromDynamicSetCall(functionCall: FirFunctionCall) =
        functionCall.dynamicVarargArguments?.lastOrNull() as? FirFunctionCall

    private fun FirStatement.unwrapDesugaredAssignmentValueReference(): FirStatement =
        (this as? FirDesugaredAssignmentValueReferenceExpression)?.expressionRef?.value ?: this

    /**
     * This function tries to "sugar back" `FirBlock`s generated in
     * [org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder.generateIncrementOrDecrementBlockForArrayAccess] and
     * [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer.transformIncrementDecrementExpression]
     */
    private fun FirBlock.tryConvertDynamicIncrementOrDecrementToIr(): IrExpression? {
        // Key observations:
        // 1. For postfix operations `<unary>` is always present and is returned in referenced as the last statement
        // 2. The second to last statement is always either a `set()` call or an assignment

        val unary = statements.findIsInstanceAnd<FirProperty> { it.name == SpecialNames.UNARY }
        // The thing `++` or `--` is called for.
        // This expression may reference things like `<array>` or `<indexN>` if it's an array access,
        // but it's definitely going to be something `++` or `--` could assign a value to
        val operationReceiver = (unary?.initializer ?: statements.lastOrNull())
            ?.unwrapDesugaredAssignmentValueReference() as? FirQualifiedAccessExpression
            ?: return null

        if (operationReceiver.resolvedType !is ConeDynamicType) {
            return null
        }

        // A node representing either `++` or `--`
        val operationCall = when (val it = statements[statements.lastIndex - 1]) {
            is FirVariableAssignment -> it.rValue as? FirFunctionCall ?: return null
            is FirFunctionCall -> extractOperationFromDynamicSetCall(it) ?: return null
            else -> return null
        }

        // `operationReceiver` can look like `s`, `r.s` or `r[s]`.
        // To generate a proper assignment, the block may want to save `r` to a separate variable
        val operationReceiverReceiver = statements
            .findIsInstanceAnd<FirProperty> { it.name == SpecialNames.RECEIVER || it.name == SpecialNames.ARRAY }?.initializer
            ?: (operationReceiver as? FirQualifiedAccessExpression)?.explicitReceiver

        // If `operationReceiver` is an array access, let's ignore its `<indexN>` arguments and
        // later manually convert them and put into the ir expression
        val isArray = statements.find { it is FirProperty && it.name == SpecialNames.ARRAY } != null

        val convertedOperationReceiver = callGenerator.convertToIrCall(
            operationReceiver, operationReceiver.resolvedType,
            convertToIrReceiverExpression(operationReceiverReceiver, operationReceiver),
            noArguments = isArray,
        ).applyIf(isArray) {
            require(this is IrDynamicOperatorExpression)

            val arrayAccess = operationReceiver as? FirFunctionCall ?: return null
            val originalVararg = arrayAccess.resolvedArgumentMapping?.keys?.filterIsInstance<FirVarargArgumentsExpression>()?.firstOrNull()

            originalVararg?.arguments?.forEach {
                val indexNVariable = (it as? FirPropertyAccessExpression)?.calleeReference?.toResolvedPropertySymbol()?.fir
                val initializer = indexNVariable?.initializer ?: return@forEach
                arguments.add(convertToIrExpression(initializer))
            }

            this
        }

        return callGenerator.convertToIrCall(
            operationCall, operationCall.resolvedType,
            convertedOperationReceiver,
        )
    }

    private fun FirBlock.convertToIrExpressionOrBlock(origin: IrStatementOrigin? = null): IrExpression {
        if (this.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
            tryConvertDynamicIncrementOrDecrementToIr()?.let {
                return it
            }
        }
        if (this is FirSingleExpressionBlock) {
            when (val stmt = statement) {
                is FirExpression -> return convertToIrExpression(stmt)
                !is FirDeclaration -> return stmt.accept(this@Fir2IrVisitor, null) as IrExpression
            }
        }
        if (source?.kind is KtRealSourceElementKind) {
            val lastStatement = statements.lastOrNull() as? FirExpression
            val lastStatementHasNothingType = lastStatement?.resolvedType?.fullyExpandedType(session)?.isNothing == true
            return statements.convertToIrBlock(source, origin, forceUnitType = origin?.isLoop == true || lastStatementHasNothingType)
        }
        return statements.convertToIrExpressionOrBlock(source, origin)
    }

    private fun FirBlock.convertToIrBlock(forceUnitType: Boolean): IrExpression {
        return statements.convertToIrBlock(source, null, forceUnitType)
    }

    private fun List<FirStatement>.convertToIrExpressionOrBlock(
        source: KtSourceElement?,
        origin: IrStatementOrigin?
    ): IrExpression {
        if (size == 1) {
            val firStatement = single()
            if (firStatement is FirExpression &&
                (firStatement !is FirBlock || firStatement.source?.kind != KtFakeSourceElementKind.DesugaredForLoop)
            ) {
                return convertToIrExpression(firStatement)
            }
        }
        return convertToIrBlock(source, origin, forceUnitType = origin?.isLoop == true)
    }

    private fun List<FirStatement>.convertToIrBlock(
        source: KtSourceElement?,
        origin: IrStatementOrigin?,
        forceUnitType: Boolean,
    ): IrExpression {
        val type = if (forceUnitType)
            builtins.unitType
        else
            (lastOrNull() as? FirExpression)?.resolvedType?.toIrType(c) ?: builtins.unitType
        return source.convertWithOffsets { startOffset, endOffset ->
            if (origin == IrStatementOrigin.DO_WHILE_LOOP) {
                IrCompositeImpl(
                    startOffset, endOffset, type, null,
                    mapToIrStatements(recognizePostfixIncDec = false).filterNotNull()
                )
            } else {
                val irStatements = mapToIrStatements()
                val singleStatement = irStatements.singleOrNull()
                if (singleStatement is IrBlock && (!forceUnitType || singleStatement.type.isUnit()) &&
                    (singleStatement.origin == IrStatementOrigin.POSTFIX_INCR || singleStatement.origin == IrStatementOrigin.POSTFIX_DECR)
                ) {
                    singleStatement
                } else {
                    val blockOrigin = if (forceUnitType && origin != IrStatementOrigin.FOR_LOOP) null else origin
                    IrBlockImpl(startOffset, endOffset, type, blockOrigin, irStatements.filterNotNull())
                }
            }
        }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Any?): IrElement {
        return errorExpression.convertWithOffsets { startOffset, endOffset ->
            IrErrorExpressionImpl(
                startOffset, endOffset,
                errorExpression.resolvedType.toIrType(c),
                errorExpression.diagnostic.reason
            )
        }
    }

    override fun visitEnumEntryDeserializedAccessExpression(
        enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression,
        data: Any?
    ): IrElement {
        return visitPropertyAccessExpression(enumEntryDeserializedAccessExpression.toQualifiedPropertyAccessExpression(session), data)
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: Any?): IrElement {
        return elvisExpression.convertWithOffsets { startOffset, endOffset ->
            val irLhsVariable = conversionScope.scope().createTemporaryVariable(
                irExpression = elvisExpression.lhs.accept(this, null) as IrExpression,
                nameHint = "elvis_lhs",
                startOffset = startOffset,
                endOffset = endOffset
            )

            fun irGetLhsValue(): IrGetValue =
                IrGetValueImpl(startOffset, endOffset, irLhsVariable.type, irLhsVariable.symbol)

            val originalType = elvisExpression.lhs.resolvedType
            val notNullType = originalType.withNullability(ConeNullability.NOT_NULL, session.typeContext)
            val irBranches = listOf(
                IrBranchImpl(
                    startOffset, endOffset,
                    primitiveOp2(
                        startOffset, endOffset, builtins.eqeqSymbol,
                        builtins.booleanType, IrStatementOrigin.EQEQ,
                        irGetLhsValue(),
                        IrConstImpl.constNull(startOffset, endOffset, builtins.nothingNType)
                    ),
                    convertToIrExpression(elvisExpression.rhs)
                        .insertImplicitCast(elvisExpression, elvisExpression.rhs.resolvedType, elvisExpression.resolvedType)
                ),
                IrElseBranchImpl(
                    IrConstImpl.boolean(startOffset, endOffset, builtins.booleanType, true),
                    if (notNullType == originalType) {
                        irGetLhsValue()
                    } else {
                        Fir2IrImplicitCastInserter.implicitCastOrExpression(
                            irGetLhsValue(),
                            originalType.toFirResolvedTypeRef().resolvedTypeFromPrototype(notNullType).toIrType(c)
                        )
                    }
                )
            )

            generateWhen(
                startOffset, endOffset, IrStatementOrigin.ELVIS,
                irLhsVariable, irBranches,
                elvisExpression.resolvedType.toIrType(c)
            )
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Any?): IrElement {
        val subjectVariable = generateWhenSubjectVariable(whenExpression)
        val origin = when (whenExpression.source?.elementType) {
            KtNodeTypes.WHEN -> IrStatementOrigin.WHEN
            KtNodeTypes.IF -> IrStatementOrigin.IF
            KtNodeTypes.BINARY_EXPRESSION -> when (whenExpression.source?.operationToken) {
                KtTokens.OROR -> IrStatementOrigin.OROR
                KtTokens.ANDAND -> IrStatementOrigin.ANDAND
                else -> null
            }
            KtNodeTypes.POSTFIX_EXPRESSION -> IrStatementOrigin.EXCLEXCL
            else -> null
        }
        return conversionScope.withWhenSubject(subjectVariable) {
            whenExpression.convertWithOffsets { startOffset, endOffset ->
                if (whenExpression.branches.isEmpty()) {
                    return@convertWithOffsets IrBlockImpl(startOffset, endOffset, builtins.unitType, origin)
                }
                val isProperlyExhaustive = whenExpression.isDeeplyProperlyExhaustive()
                val whenExpressionType =
                    if (isProperlyExhaustive) whenExpression.resolvedType else session.builtinTypes.unitType.type
                val irBranches = whenExpression.convertWhenBranchesTo(
                    mutableListOf(),
                    whenExpressionType,
                    flattenElse = origin == IrStatementOrigin.IF,
                )
                if (isProperlyExhaustive && whenExpression.branches.none { it.condition is FirElseIfTrueCondition }) {
                    val irResult = IrCallImpl(
                        startOffset, endOffset, builtins.nothingType,
                        builtins.noWhenBranchMatchedExceptionSymbol,
                        typeArgumentsCount = 0,
                        valueArgumentsCount = 0
                    )
                    irBranches += IrElseBranchImpl(
                        IrConstImpl.boolean(startOffset, endOffset, builtins.booleanType, true), irResult
                    )
                }
                generateWhen(startOffset, endOffset, origin, subjectVariable, irBranches, whenExpressionType.toIrType(c))
            }
        }.also {
            whenExpression.accept(implicitCastInserter, it)
        }
    }

    /**
     * TODO this shouldn't be required anymore once KT-65997 is fixed.
     */
    private fun FirWhenExpression.isDeeplyProperlyExhaustive(): Boolean {
        if (!isProperlyExhaustive) {
            return false
        }

        val nestedElseIfExpression = branches.lastOrNull()?.nestedElseIfOrNull() ?: return true
        return nestedElseIfExpression.isDeeplyProperlyExhaustive()
    }

    /**
     * Converts the branches to [IrBranch]es.
     *
     * If [flattenElse] is `true` and the else branch contains another [FirWhenExpression] that's built from an `if`,
     * its branches will be added directly to the [result] list instead.
     *
     * TODO this shouldn't be required anymore once KT-65997 is fixed.
     */
    private fun FirWhenExpression.convertWhenBranchesTo(
        result: MutableList<IrBranch>,
        whenExpressionType: ConeKotlinType,
        flattenElse: Boolean,
    ): MutableList<IrBranch> {
        for (branch in branches) {
            if (flattenElse) {
                val elseIfExpression = branch.nestedElseIfOrNull()
                if (elseIfExpression != null) {
                    elseIfExpression.convertWhenBranchesTo(result, whenExpressionType, flattenElse = true)
                    break
                }
            }

            result.add(branch.toIrWhenBranch(whenExpressionType))
        }

        return result
    }

    private fun FirWhenBranch.nestedElseIfOrNull(): FirWhenExpression? {
        if (condition is FirElseIfTrueCondition) {
            val elseWhenExpression = (result as? FirSingleExpressionBlock)?.statement as? FirWhenExpression
            if (elseWhenExpression != null && elseWhenExpression.source?.elementType == KtNodeTypes.IF) {
                return elseWhenExpression
            }
        }

        return null
    }

    private fun generateWhen(
        startOffset: Int,
        endOffset: Int,
        origin: IrStatementOrigin?,
        subjectVariable: IrVariable?,
        branches: List<IrBranch>,
        resultType: IrType
    ): IrExpression {
        // Note: ELVIS origin is set only on wrapping block
        val irWhen = IrWhenImpl(startOffset, endOffset, resultType, origin.takeIf { it != IrStatementOrigin.ELVIS }, branches)
        return if (subjectVariable == null) {
            irWhen
        } else {
            IrBlockImpl(startOffset, endOffset, irWhen.type, origin, listOf(subjectVariable, irWhen))
        }
    }

    private fun generateWhenSubjectVariable(whenExpression: FirWhenExpression): IrVariable? {
        val subjectVariable = whenExpression.subjectVariable
        val subjectExpression = whenExpression.subject
        return when {
            subjectVariable != null -> subjectVariable.accept(this, null) as IrVariable
            subjectExpression != null ->
                conversionScope.scope().createTemporaryVariable(convertToIrExpression(subjectExpression), "subject")
            else -> null
        }
    }

    private fun FirWhenBranch.toIrWhenBranch(whenExpressionType: ConeKotlinType): IrBranch {
        return convertWithOffsets { startOffset, _ ->
            val condition = condition
            val irResult = convertToIrExpression(result).insertImplicitCast(result, result.resolvedType, whenExpressionType)
            if (condition is FirElseIfTrueCondition) {
                IrElseBranchImpl(IrConstImpl.boolean(irResult.startOffset, irResult.endOffset, builtins.booleanType, true), irResult)
            } else {
                IrBranchImpl(startOffset, irResult.endOffset, convertToIrExpression(condition), irResult)
            }
        }
    }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: Any?): IrElement {
        val lastSubjectVariable = conversionScope.lastWhenSubject()
        return whenSubjectExpression.convertWithOffsets { startOffset, endOffset ->
            IrGetValueImpl(startOffset, endOffset, lastSubjectVariable.type, lastSubjectVariable.symbol)
        }
    }

    private val loopMap = mutableMapOf<FirLoop, IrLoop>()

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Any?): IrElement {
        val irLoop = doWhileLoop.convertWithOffsets { startOffset, endOffset ->
            IrDoWhileLoopImpl(
                startOffset, endOffset, builtins.unitType,
                IrStatementOrigin.DO_WHILE_LOOP
            ).apply {
                loopMap[doWhileLoop] = this
                label = doWhileLoop.label?.name
                body = runUnless(doWhileLoop.block is FirEmptyExpressionBlock) {
                    Fir2IrImplicitCastInserter.coerceToUnitIfNeeded(
                        doWhileLoop.block.convertToIrExpressionOrBlock(origin),
                        builtins
                    )
                }
                condition = convertToIrExpression(doWhileLoop.condition)
                loopMap.remove(doWhileLoop)
            }
        }.also {
            doWhileLoop.accept(implicitCastInserter, it)
        }
        return IrBlockImpl(irLoop.startOffset, irLoop.endOffset, builtins.unitType).apply {
            statements.add(irLoop)
        }
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: Any?): IrElement {
        return whileLoop.convertWithOffsets { startOffset, endOffset ->
            val isForLoop = whileLoop.source?.elementType == KtNodeTypes.FOR
            val origin = if (isForLoop) IrStatementOrigin.FOR_LOOP_INNER_WHILE else IrStatementOrigin.WHILE_LOOP
            val firLoopBody = whileLoop.block
            IrWhileLoopImpl(startOffset, endOffset, builtins.unitType, origin).apply {
                loopMap[whileLoop] = this
                label = whileLoop.label?.name
                condition = convertToIrExpression(whileLoop.condition)
                body = runUnless(firLoopBody is FirEmptyExpressionBlock) {
                    if (isForLoop) {
                        /*
                         * for loops in IR must have their body in the exact following form
                         * because some of the lowerings (e.g. `ForLoopLowering`) expect it:
                         *
                         * for (x in list) { ...body...}
                         *
                         * IR (loop body):
                         *   IrBlock:
                         *     x = <iterator>.next()
                         *     ... possible destructured loop variables, in case iterator is a tuple: `for ((a,b,c) in list) { ...body...}` ...
                         *     IrBlock:
                         *         ...body...
                         */
                        firLoopBody.convertWithOffsets { innerStartOffset, innerEndOffset ->
                            val loopBodyStatements = firLoopBody.statements
                            val firLoopVarStmt = loopBodyStatements.firstOrNull()
                                ?: error("Unexpected shape of for loop body: missing body statements")

                            val (destructuredLoopVariables, realStatements) = loopBodyStatements.drop(1).partition {
                                it is FirProperty && it.initializer is FirComponentCall
                            }
                            val firExpression = realStatements.singleOrNull() as? FirExpression
                                ?: error("Unexpected shape of for loop body: must be single real loop statement, but got ${realStatements.size}")

                            val irStatements = buildList {
                                addIfNotNull(firLoopVarStmt.toIrStatement())
                                destructuredLoopVariables.forEach { addIfNotNull(it.toIrStatement()) }
                                if (firExpression !is FirEmptyExpressionBlock) {
                                    add(convertToIrExpression(firExpression))
                                }
                            }

                            IrBlockImpl(
                                innerStartOffset,
                                innerEndOffset,
                                builtins.unitType,
                                origin,
                                irStatements,
                            )
                        }
                    } else {
                        Fir2IrImplicitCastInserter.coerceToUnitIfNeeded(
                            firLoopBody.convertToIrExpressionOrBlock(origin),
                            builtins
                        )
                    }
                }
                loopMap.remove(whileLoop)
            }
        }.also {
            whileLoop.accept(implicitCastInserter, it)
        }
    }

    private fun FirJump<FirLoop>.convertJumpWithOffsets(
        f: (startOffset: Int, endOffset: Int, irLoop: IrLoop, label: String?) -> IrBreakContinue
    ): IrExpression {
        return convertWithOffsets { startOffset, endOffset ->
            val firLoop = target.labeledElement
            val irLoop = loopMap[firLoop]
            if (irLoop == null) {
                IrErrorExpressionImpl(startOffset, endOffset, builtins.nothingType, "Unbound loop: ${render()}")
            } else {
                f(startOffset, endOffset, irLoop, irLoop.label.takeIf { target.labelName != null })
            }
        }
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Any?): IrElement {
        return breakExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop, label ->
            IrBreakImpl(startOffset, endOffset, builtins.nothingType, irLoop).apply {
                this.label = label
            }
        }
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Any?): IrElement {
        return continueExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop, label ->
            IrContinueImpl(startOffset, endOffset, builtins.nothingType, irLoop).apply {
                this.label = label
            }
        }
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Any?): IrElement {
        return throwExpression.convertWithOffsets { startOffset, endOffset ->
            IrThrowImpl(startOffset, endOffset, builtins.nothingType, convertToIrExpression(throwExpression.exception))
        }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: Any?): IrElement {
        // Always generate a block for try, catch and finally blocks. When leaving the finally block in the debugger
        // for both Java and Kotlin there is a step on the end brace. For that to happen we need the block with
        // that line number for the finally block.
        return tryExpression.convertWithOffsets { startOffset, endOffset ->
            IrTryImpl(
                startOffset, endOffset, tryExpression.resolvedType.toIrType(c),
                tryExpression.tryBlock.convertToIrBlock(forceUnitType = false),
                tryExpression.catches.map { it.accept(this, data) as IrCatch },
                tryExpression.finallyBlock?.convertToIrBlock(forceUnitType = true)
            )
        }.also {
            tryExpression.accept(implicitCastInserter, it)
        }
    }

    override fun visitCatch(catch: FirCatch, data: Any?): IrElement {
        return catch.convertWithOffsets { startOffset, endOffset ->
            val catchParameter = declarationStorage.createAndCacheIrVariable(
                catch.parameter, conversionScope.parentFromStack(), IrDeclarationOrigin.CATCH_PARAMETER
            )
            IrCatchImpl(startOffset, endOffset, catchParameter, catch.block.convertToIrBlock(forceUnitType = false))
        }
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Any?): IrElement =
        operatorGenerator.convertComparisonExpression(comparisonExpression)

    override fun visitStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall,
        data: Any?
    ): IrElement = whileAnalysing(session, stringConcatenationCall) {
        return stringConcatenationCall.convertWithOffsets { startOffset, endOffset ->
            val arguments = mutableListOf<IrExpression>()
            val sb = StringBuilder()
            var startArgumentOffset = -1
            var endArgumentOffset = -1
            for (firArgument in stringConcatenationCall.arguments) {
                val argument = convertToIrExpression(firArgument)
                if (argument is IrConst<*>) {
                    if (sb.isEmpty()) {
                        startArgumentOffset = argument.startOffset
                    }
                    sb.append(argument.value)
                    endArgumentOffset = argument.endOffset
                } else {
                    if (sb.isNotEmpty()) {
                        arguments += IrConstImpl.string(startArgumentOffset, endArgumentOffset, builtins.stringType, sb.toString())
                        sb.clear()
                    }
                    arguments += argument
                }
            }
            if (sb.isNotEmpty()) {
                arguments += IrConstImpl.string(startArgumentOffset, endArgumentOffset, builtins.stringType, sb.toString())
            }
            IrStringConcatenationImpl(startOffset, endOffset, builtins.stringType, arguments)
        }
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): IrElement {
        return typeOperatorCall.convertWithOffsets { startOffset, endOffset ->
            val irTypeOperand = typeOperatorCall.conversionTypeRef.toIrType(c)
            val (irType, irTypeOperator) = when (typeOperatorCall.operation) {
                FirOperation.IS -> builtins.booleanType to IrTypeOperator.INSTANCEOF
                FirOperation.NOT_IS -> builtins.booleanType to IrTypeOperator.NOT_INSTANCEOF
                FirOperation.AS -> irTypeOperand to IrTypeOperator.CAST
                FirOperation.SAFE_AS -> irTypeOperand.makeNullable() to IrTypeOperator.SAFE_CAST
                else -> TODO("Should not be here: ${typeOperatorCall.operation} in type operator call")
            }

            IrTypeOperatorCallImpl(
                startOffset, endOffset, irType, irTypeOperator, irTypeOperand,
                convertToIrExpression(typeOperatorCall.argument)
            )
        }
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Any?): IrElement {
        return whileAnalysing(session, equalityOperatorCall) {
            operatorGenerator.convertEqualityOperatorCall(equalityOperatorCall)
        }
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: Any?): IrElement = whileAnalysing(
        session, checkNotNullCall
    ) {
        return checkNotNullCall.convertWithOffsets { startOffset, endOffset ->
            IrCallImpl(
                startOffset, endOffset,
                checkNotNullCall.resolvedType.toIrType(c),
                builtins.checkNotNullSymbol,
                typeArgumentsCount = 1,
                valueArgumentsCount = 1,
                origin = IrStatementOrigin.EXCLEXCL
            ).apply {
                putTypeArgument(0, checkNotNullCall.argument.resolvedType.toIrType(c).makeNotNull())
                putValueArgument(0, convertToIrExpression(checkNotNullCall.argument))
            }
        }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Any?): IrElement = whileAnalysing(session, getClassCall) {
        val argument = getClassCall.argument
        val irType = getClassCall.resolvedType.toIrType(c)
        val irClassType =
            if (argument is FirClassReferenceExpression) {
                argument.classTypeRef.toIrType(c)
            } else {
                argument.resolvedType.toIrType(c)
            }
        val irClassReferenceSymbol = when (argument) {
            is FirResolvedReifiedParameterReference -> {
                classifierStorage.getIrTypeParameterSymbol(argument.symbol, ConversionTypeOrigin.DEFAULT)
            }
            is FirResolvedQualifier -> {
                when (val symbol = argument.symbol) {
                    is FirClassSymbol -> {
                        classifierStorage.getIrClassSymbol(symbol)
                    }
                    is FirTypeAliasSymbol -> {
                        symbol.fir.fullyExpandedConeType(session).toIrClassSymbol()
                    }
                    else ->
                        return getClassCall.convertWithOffsets { startOffset, endOffset ->
                            IrErrorCallExpressionImpl(
                                startOffset, endOffset, irType, "Resolved qualifier ${argument.render()} does not have correct symbol"
                            )
                        }
                }
            }
            is FirClassReferenceExpression -> {
                (argument.classTypeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType)?.toIrClassSymbol()
                // A null value means we have some unresolved code, possibly in a binary dependency that's missing a transitive dependency,
                // see KT-60181.
                // Returning null will lead to convertToIrExpression(argument) being called below which leads to a crash.
                // Instead, we return an error symbol.
                    ?: IrErrorClassImpl.symbol
            }
            else -> null
        }
        return getClassCall.convertWithOffsets { startOffset, endOffset ->
            if (irClassReferenceSymbol != null) {
                IrClassReferenceImpl(startOffset, endOffset, irType, irClassReferenceSymbol, irClassType)
            } else {
                IrGetClassImpl(startOffset, endOffset, irType, convertToIrExpression(argument))
            }
        }
    }

    private fun ConeClassLikeType?.toIrClassSymbol(): IrClassSymbol? =
        (this?.lookupTag?.toSymbol(session) as? FirClassSymbol<*>)?.let {
            classifierStorage.getIrClassSymbol(it)
        }

    private fun convertToArrayLiteral(
        arrayLiteral: FirArrayLiteral,
        // See comment to convertToIrExpression
        expectedType: ConeKotlinType?,
    ): IrVararg {
        return arrayLiteral.convertWithOffsets { startOffset, endOffset ->
            val arrayType = (expectedType ?: arrayLiteral.resolvedType).toIrType(c)
            val elementType = arrayType.getArrayElementType(builtins)
            IrVarargImpl(
                startOffset, endOffset,
                type = arrayType,
                varargElementType = elementType,
                elements = arrayLiteral.arguments.map { it.convertToIrVarargElement() }
            )
        }
    }

    override fun visitIndexedAccessAugmentedAssignment(
        indexedAccessAugmentedAssignment: FirIndexedAccessAugmentedAssignment,
        data: Any?
    ): IrElement = whileAnalysing(session, indexedAccessAugmentedAssignment) {
        return indexedAccessAugmentedAssignment.convertWithOffsets { startOffset, endOffset ->
            IrErrorCallExpressionImpl(
                startOffset, endOffset, builtins.unitType,
                "FirIndexedAccessAugmentedAssignment (resolve isn't supported yet)"
            )
        }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Any?): IrElement {
        return callGenerator.convertToGetObject(resolvedQualifier)
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: Any?): IrElement {
        // Support for error suppression case
        return visitResolvedQualifier(errorResolvedQualifier, data)
    }

    private fun LogicOperationKind.toIrDynamicOperator() = when (this) {
        LogicOperationKind.AND -> IrDynamicOperator.ANDAND
        LogicOperationKind.OR -> IrDynamicOperator.OROR
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Any?): IrElement {
        return binaryLogicExpression.convertWithOffsets<IrElement> { startOffset, endOffset ->
            val leftOperand = binaryLogicExpression.leftOperand.accept(this, data) as IrExpression
            val rightOperand = binaryLogicExpression.rightOperand.accept(this, data) as IrExpression
            if (leftOperand.type is IrDynamicType) {
                IrDynamicOperatorExpressionImpl(
                    startOffset,
                    endOffset,
                    builtins.booleanType,
                    binaryLogicExpression.kind.toIrDynamicOperator(),
                ).apply {
                    receiver = leftOperand
                    arguments.add(rightOperand)
                }
            } else when (binaryLogicExpression.kind) {
                LogicOperationKind.AND -> {
                    IrIfThenElseImpl(startOffset, endOffset, builtins.booleanType, IrStatementOrigin.ANDAND).apply {
                        branches.add(IrBranchImpl(leftOperand, rightOperand))
                        branches.add(elseBranch(constFalse(rightOperand.startOffset, rightOperand.endOffset)))
                    }
                }
                LogicOperationKind.OR -> {
                    IrIfThenElseImpl(startOffset, endOffset, builtins.booleanType, IrStatementOrigin.OROR).apply {
                        branches.add(IrBranchImpl(leftOperand, constTrue(leftOperand.startOffset, leftOperand.endOffset)))
                        branches.add(elseBranch(rightOperand))
                    }
                }
            }
        }
    }
}

val KtSourceElement.isChildOfForLoop: Boolean
    get() =
        if (this is KtPsiSourceElement) psi.parent is KtForExpression
        else treeStructure.getParent(lighterASTNode)?.tokenType == KtNodeTypes.FOR

val KtSourceElement.operationToken: IElementType?
    get() {
        assert(elementType == KtNodeTypes.BINARY_EXPRESSION)
        return if (this is KtPsiSourceElement) (psi as? KtBinaryExpression)?.operationToken
        else treeStructure.findChildByType(lighterASTNode, KtNodeTypes.OPERATION_REFERENCE)?.tokenType
            ?: error("No operation reference for binary expression: $lighterASTNode")
    }
