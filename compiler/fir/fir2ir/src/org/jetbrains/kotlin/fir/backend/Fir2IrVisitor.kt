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
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.generators.ClassMemberGenerator
import org.jetbrains.kotlin.fir.backend.generators.OperatorExpressionGenerator
import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.deserialization.toQualifiedPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.scriptResolutionHacksComponent
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.whileAnalysing
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorClassImpl
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultConstructor
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.findIsInstanceAnd

class Fir2IrVisitor(
    private val c: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by c, FirDefaultVisitor<IrElement, Any?>() {
    private val cleaner: FirDeclarationsContentCleaner = FirDeclarationsContentCleaner.create()
    private val memberGenerator = ClassMemberGenerator(c, this, conversionScope, cleaner)

    private val operatorGenerator = OperatorExpressionGenerator(c, this, conversionScope)
    private var _annotationMode: Boolean = false

    val annotationMode: Boolean
        get() = _annotationMode
    private val unitType: ConeClassLikeType = session.builtinTypes.unitType.coneType

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
        cleaner.cleanFile(file)
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
        if (configuration.skipBodies) return irEnumEntry

        val correspondingClass = irEnumEntry.correspondingClass
        val initializer = enumEntry.initializer
        val irType = enumEntry.returnTypeRef.toIrType()
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
                irEnumEntry.initializerExpression = IrFactoryImpl.createExpressionBody(
                    IrEnumConstructorCallImpl(
                        startOffset, endOffset, irType,
                        constructor.symbol,
                        typeArgumentsCount = constructor.typeParameters.size,
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
                    irEnumEntry.initializerExpression = IrFactoryImpl.createExpressionBody(
                        delegatedConstructor.convertWithOffsets { startOffset, endOffset ->
                            delegatedConstructor.toIrDelegatingConstructorCall(startOffset, endOffset)
                        }
                    )
                }
            }
        } else if (irParentEnumClass != null && !irParentEnumClass.isExpect && initializer == null) {
            // a default-ish enum entry whose initializer would be a delegating constructor call
            // `irParentEnumClass` definitely is not a lazy class
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val constructor = irParentEnumClass.defaultConstructor
                ?: error("Assuming that default constructor should exist and be converted at this point: ${enumEntry.render()}")
            enumEntry.convertWithOffsets { startOffset, endOffset ->
                irEnumEntry.initializerExpression = IrFactoryImpl.createExpressionBody(
                    IrEnumConstructorCallImpl(
                        startOffset, endOffset, irType, constructor.symbol,
                        typeArgumentsCount = constructor.typeParameters.size
                    )
                )
                irEnumEntry
            }
        }
        cleaner.cleanEnumEntry(enumEntry)
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
            irClass.sealedSubclasses = regularClass.getIrSymbolsForSealedSubclasses()
        }
        conversionScope.withParent(irClass) {
            memberGenerator.convertClassContent(irClass, regularClass)
        }
        cleaner.cleanClass(regularClass)
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
                        IrFactoryImpl.createValueParameter(
                            startOffset, endOffset, origin, IrParameterKind.Regular, name, receiver.typeRef.toIrType(),
                            isAssignable = false,
                            IrValueParameterSymbolImpl(),
                            varargElementType = null, isCrossinline = false, isNoinline = false, isHidden = false
                        ).also {
                            it.parent = irScript
                            if (!isSelf) {
                                @OptIn(DelicateIrParameterIndexSetter::class)
                                it.indexInParameters = index
                            }
                        }
                    }
                if (isSelf) {
                    irReceiver.kind = IrParameterKind.DispatchReceiver
                    irScript.thisReceiver = irReceiver
                    irScript.baseClass = irReceiver.type
                    null
                } else irReceiver
            }

            conversionScope.withParent(irScript) {
                val destructComposites = mutableMapOf<FirVariableSymbol<*>, IrComposite>()
                for (statement in script.declarations) {
                    if (statement !is FirAnonymousInitializer) {
                        val irStatement = when (statement) {
                            is FirProperty if statement.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR -> {
                                val convertedInitializer = when {
                                    statement.isUnnamedLocalVariable -> statement.initializer?.accept(this@Fir2IrVisitor, null)
                                    else -> null
                                }

                                if (convertedInitializer is IrStatement && statement.destructuringDeclarationContainerVariable != null) {
                                    // In name-based destructuring, underscores don't produce variables,
                                    // but the call to the initializer must be preserved.
                                    val correspondingComposite = destructComposites[statement.destructuringDeclarationContainerVariable!!]!!
                                    correspondingComposite.statements.add(convertedInitializer)
                                    continue
                                }

                                convertedInitializer as? IrStatement ?: continue
                            }
                            is FirProperty if statement.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> {
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
                            is FirVariable if statement.isDestructuringDeclarationContainerVariable == true -> {
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
                            is FirProperty if statement.destructuringDeclarationContainerVariable != null -> {
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
                            is FirClass -> {
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
            irFunction.body = if (configuration.skipBodies) {
                IrFactoryImpl.createExpressionBody(IrConstImpl.defaultValueForType(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.returnType))
            } else {
                val irBlock = codeFragment.block.convertToIrBlock(origin = null, expectedType = null)
                IrFactoryImpl.createExpressionBody(irBlock)
            }
        }
        declarationStorage.leaveScope(irFunction.symbol)

        return irFunction
    }

    override fun visitReplSnippet(
        replSnippet: FirReplSnippet,
        data: Any?,
    ): IrElement {
        val irSnippet = declarationStorage.getCachedIrReplSnippet(replSnippet)!!
        irSnippet.parent = conversionScope.parentFromStack()
        declarationStorage.enterScope(irSnippet.symbol)

        for (configurator in session.extensionService.fir2IrReplSnippetConfigurators) {
            with(configurator) {
                prepareSnippet(this@Fir2IrVisitor, replSnippet, irSnippet)
            }
        }

        irSnippet.receiverParameters = replSnippet.receivers.mapIndexed { index, receiver ->
            val name = Name.identifier("${SCRIPT_RECEIVER_NAME_PREFIX}_$index")
            val origin = IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER
            receiver.convertWithOffsets { startOffset, endOffset ->
                IrFactoryImpl.createValueParameter(
                    startOffset, endOffset, origin, IrParameterKind.Context, name, receiver.typeRef.toIrType(), isAssignable = false,
                    IrValueParameterSymbolImpl(),
                    varargElementType = null, isCrossinline = false, isNoinline = false, isHidden = false
                ).also {
                    it.parent = irSnippet
                    @OptIn(DelicateIrParameterIndexSetter::class)
                    it.indexInParameters = index
                }
            }
        }
        conversionScope.withParent(irSnippet) {
            irSnippet.body = convertToIrBlockBody(replSnippet.body)
        }

        declarationStorage.leaveScope(irSnippet.symbol)

        return irSnippet
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
        }.also {
            cleaner.cleanAnonymousObject(anonymousObject)
        }
    }

    // ==================================================================================

    override fun visitConstructor(constructor: FirConstructor, data: Any?): IrElement = whileAnalysing(session, constructor) {
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val irConstructor = declarationStorage.getCachedIrConstructorSymbol(constructor)!!.owner
        return conversionScope.withFunction(irConstructor) {
            memberGenerator.convertFunctionContent(irConstructor, constructor, containingClass = conversionScope.containerFirClass())
        }.also {
            cleaner.cleanConstructor(constructor)
        }
    }

    override fun visitAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: Any?
    ): IrElement = whileAnalysing(session, anonymousInitializer) {
        val irAnonymousInitializer = declarationStorage.getIrAnonymousInitializer(anonymousInitializer)
        declarationStorage.enterScope(irAnonymousInitializer.symbol)
        conversionScope.withInitBlock(irAnonymousInitializer) {
            irAnonymousInitializer.body =
                if (configuration.skipBodies) IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                else convertToIrBlockBody(anonymousInitializer.body!!)
        }
        declarationStorage.leaveScope(irAnonymousInitializer.symbol)
        cleaner.cleanAnonymousInitializer(anonymousInitializer)
        return irAnonymousInitializer
    }

    override fun visitNamedFunction(namedFunction: FirNamedFunction, data: Any?): IrElement = whileAnalysing(session, namedFunction) {
        val irFunction = if (namedFunction.visibility == Visibilities.Local) {
            declarationStorage.createAndCacheIrFunction(
                namedFunction, irParent = conversionScope.parent(), predefinedOrigin = IrDeclarationOrigin.LOCAL_FUNCTION, isLocal = true
            )
        } else {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            declarationStorage.getCachedIrFunctionSymbol(namedFunction)!!.owner
        }
        return conversionScope.withFunction(irFunction) {
            memberGenerator.convertFunctionContent(
                irFunction, namedFunction, containingClass = conversionScope.containerFirClass()
            )
        }.also {
            cleaner.cleanNamedFunction(namedFunction)
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

            val type = anonymousFunction.typeRef.coneType.approximateFunctionTypeInputs().toIrType()

            IrFunctionExpressionImpl(
                startOffset, endOffset, type, irFunction,
                if (irFunction.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) IrStatementOrigin.LAMBDA
                else IrStatementOrigin.ANONYMOUS_FUNCTION
            )
        }.also {
            cleaner.cleanAnonymousFunction(anonymousFunction)
        }
    }

    private fun visitLocalVariable(variable: FirProperty): IrElement = whileAnalysing(session, variable) {
        assert(variable.symbol is FirLocalPropertySymbol)
        val delegate = variable.delegate
        if (delegate != null) {
            val irProperty = declarationStorage.createAndCacheIrLocalDelegatedProperty(variable, conversionScope.parentFromStack())
            val irDelegate = irProperty.delegate
            requireNotNull(irDelegate) { "Local delegated property ${irProperty.render()} has no delegate" }
            irDelegate.initializer = convertToIrExpression(delegate, isDelegate = true)
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
            val convertedInitializer = convertToIrExpression(initializer, expectedType = variable.returnTypeRef.coneType)
            // In FIR smart-casted types from initializers of variables are not saved to the types of the variables themselves.
            // Ensuring the IrVariable's type of an implicit when-subject is as narrow as that of the initializer is important,
            // for example, for `ieee754` comparisons of `Double`s.
            if (irVariable.name == SpecialNames.WHEN_SUBJECT) {
                irVariable.type = convertedInitializer.type
            }
            irVariable.initializer = convertedInitializer
        }
        annotationGenerator.generate(irVariable, variable)
        return irVariable
    }

    override fun visitProperty(property: FirProperty, data: Any?): IrElement = whileAnalysing(session, property) {
        if (property.symbol is FirLocalPropertySymbol) return visitLocalVariable(property)
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val irProperty = declarationStorage.getCachedIrPropertySymbol(property, fakeOverrideOwnerLookupTag = null)?.owner
            ?: return IrErrorExpressionImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT),
                "Stub for Enum.entries"
            )
        return conversionScope.withProperty(irProperty, property) {
            memberGenerator.convertPropertyContent(irProperty, property)
        }.also {
            cleaner.cleanProperty(property)
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
            val value = convertToIrExpression(result, expectedType = returnExpression.target.labeledElement.returnTypeRef.coneType)
            IrReturnImpl(returnStartOffset, endOffset, builtins.nothingType, irTarget, value)
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
                varargArgumentsExpression.resolvedType.toIrType(),
                varargArgumentsExpression.coneElementTypeOrNull?.toIrType()
                    ?: error("Vararg expression has incorrect type: ${varargArgumentsExpression.render()}"),
                varargArgumentsExpression.arguments.mapNotNull {
                    if (isGetClassOfUnresolvedTypeInAnnotation(it)) null
                    else it.convertToIrVarargElement()
                }
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
            ?: error("Converting dynamic array access should have resulted in IrDynamicOperatorExpression: ${functionCall.render()}")
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
        }.let {
            Fir2IrImplicitCastInserter.implicitCastOrExpression(it, it.type.makeNotNull())
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

        callGenerator.injectGetValueCall(thisReceiverExpression, calleeReference)?.let { return it }

        when (val declarationSymbol = calleeReference.referencedMemberSymbol) {
            is FirClassSymbol -> generateThisReceiverAccessForClass(thisReceiverExpression, declarationSymbol)
            is FirScriptSymbol -> generateThisReceiverAccessForScript(thisReceiverExpression, declarationSymbol)
            is FirReplSnippetSymbol -> generateThisReceiverAccessForReplSnippet(thisReceiverExpression, declarationSymbol)
            is FirCallableSymbol -> generateThisReceiverAccessForCallable(thisReceiverExpression, declarationSymbol)
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
                val irType = firClassSymbol.defaultType().toIrType()
                IrGetObjectValueImpl(startOffset, endOffset, irType, irClassSymbol)
            }
        }

        val irClass = conversionScope.findDeclarationInParentsStack<IrClass>(irClassSymbol)

        val dispatchReceiver = conversionScope.dispatchReceiverParameter(irClass) ?: return null
        val origin = if (thisReceiverExpression.isImplicit) IrStatementOrigin.IMPLICIT_ARGUMENT else null
        return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
            callGenerator.findInjectedValue(calleeReference)?.let {
                callGenerator.useInjectedValue(it, calleeReference, startOffset, endOffset)
            } ?: IrGetValueImpl(startOffset, endOffset, dispatchReceiver.type, dispatchReceiver.symbol, origin)
        }
    }

    private fun generateThisReceiverAccessForScript(
        thisReceiverExpression: FirThisReceiverExpression,
        firScriptSymbol: FirScriptSymbol
    ): IrElement {
        val calleeReference = thisReceiverExpression.calleeReference
        val firScript = firScriptSymbol.fir
        val origin = if (thisReceiverExpression.isImplicit) IrStatementOrigin.IMPLICIT_ARGUMENT else null
        val irScript = declarationStorage.getCachedIrScript(firScript) ?: error("IrScript for ${firScript.name} not found")
        val contextParameterNumber = firScriptSymbol.fir.receivers.indexOf(calleeReference.boundSymbol?.fir)
        val receiverParameter =
            irScript.implicitReceiversParameters.find { it.indexInParameters == contextParameterNumber } ?: irScript.thisReceiver
        if (receiverParameter != null) {
            return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                IrGetValueImpl(startOffset, endOffset, receiverParameter.type, receiverParameter.symbol, origin)
            }
        } else {
            error("No script receiver found") // TODO: check if any valid situations possible here
        }
    }

    private fun generateThisReceiverAccessForReplSnippet(
        thisReceiverExpression: FirThisReceiverExpression,
        firSnippetSymbol: FirReplSnippetSymbol
    ): IrElement {
        val calleeReference = thisReceiverExpression.calleeReference
        val firSnippet = firSnippetSymbol.fir
        val origin = if (thisReceiverExpression.isImplicit) IrStatementOrigin.IMPLICIT_ARGUMENT else null
        val irSnippet = declarationStorage.getCachedIrReplSnippet(firSnippet) ?: error("IrReplSnippet for ${firSnippet.name} not found")
        val contextParameterNumber = firSnippetSymbol.fir.receivers.indexOf(calleeReference.boundSymbol?.fir)
        val receiverParameter =
            irSnippet.receiverParameters.find { it.indexInParameters == contextParameterNumber }
        if (receiverParameter != null) {
            return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                IrGetValueImpl(startOffset, endOffset, receiverParameter.type, receiverParameter.symbol, origin)
            }
        } else {
            error("Unexpected REPL snippet receiver")
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun generateThisReceiverAccessForCallable(
        thisReceiverExpression: FirThisReceiverExpression,
        firCallableSymbol: FirCallableSymbol<*>
    ): IrElement? {
        val calleeReference = thisReceiverExpression.calleeReference
        val origin = if (thisReceiverExpression.isImplicit) IrStatementOrigin.IMPLICIT_ARGUMENT else null
        callGenerator.injectGetValueCall(thisReceiverExpression, calleeReference)?.let { return it }

        val irFunction = when (firCallableSymbol) {
            is FirFunctionSymbol -> {
                val functionSymbol = declarationStorage.getIrFunctionSymbol(firCallableSymbol)
                conversionScope.findDeclarationInParentsStack<IrSimpleFunction>(functionSymbol)
            }
            is FirPropertySymbol ->
                when (val property = declarationStorage.getIrPropertySymbol(firCallableSymbol)) {
                    is IrPropertySymbol -> conversionScope.parentAccessorOfPropertyFromStack(property)
                        // TODO: the following change should be reverted, along with the one in [parentAccessorOfPropertyFromStack] on fixing KT-79107
                        ?: if (firCallableSymbol.fir.isScriptTopLevelDeclaration != true && session.scriptResolutionHacksComponent?.skipTowerDataCleanupForTopLevelInitializers == true) {
                            error("Accessor of property ${property.owner.render()} not found on parent stack")
                        } else null
                    is IrLocalDelegatedPropertySymbol -> conversionScope.parentAccessorOfDelegatedPropertyFromStack(property)
                    else -> null
                }
            else -> null
        } ?: return null

        val receiver = irFunction.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver } ?: return null

        return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
            IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol, origin)
        }
    }

    override fun visitInaccessibleReceiverExpression(
        inaccessibleReceiverExpression: FirInaccessibleReceiverExpression,
        data: Any?,
    ): IrElement {
        return inaccessibleReceiverExpression.convertWithOffsets { startOffset, endOffset ->
            IrErrorExpressionImpl(
                startOffset, endOffset,
                inaccessibleReceiverExpression.resolvedType.toIrType(),
                "Receiver is inaccessible"
            )
        }
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: Any?): IrElement {
        // Generate the expression with the original type and then cast it to the smart cast type.
        val value = convertToIrExpression(smartCastExpression.originalExpression)
        return implicitCastInserter.handleSmartCastExpression(smartCastExpression, value)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Any?): IrElement {
        return whileAnalysing(session, callableReferenceAccess) {
            convertCallableReferenceAccess(callableReferenceAccess, false)
        }
    }

    private fun convertCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, isDelegate: Boolean): IrExpression {
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
        return literalExpression.toIrConst(literalExpression.resolvedType.toIrType())
    }

    // ==================================================================================

    private fun FirStatement.toIrStatement(): IrStatement? {
        return when (this) {
            is FirTypeAlias -> null
            is FirUnitExpression -> runUnless(source?.kind is KtFakeSourceElementKind.ImplicitUnit.IndexedAssignmentCoercion) {
                convertToIrExpression(this)
            }
            is FirContractCallBlock -> null
            is FirBlock -> convertToIrExpression(this)
            is FirProperty if name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR -> when {
                !isUnnamedLocalVariable -> null
                else -> initializer?.accept(this@Fir2IrVisitor, null) as IrStatement
            }
            else -> accept(this@Fir2IrVisitor, null) as IrStatement
        }
    }

    private val FirProperty.isUnnamedLocalVariable: Boolean
        get() = initializer.let { it != null && it.source?.kind !is KtFakeSourceElementKind.DesugaredComponentFunctionCall }

    internal fun convertToIrExpression(
        expression: FirExpression,
        isDelegate: Boolean = false,
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
                expression.convertToIrExpressionOrBlock(
                    origin,
                    // We only pass the expected type if it's Unit to trigger coercion to Unit.
                    // In all other cases, the block should have the type of the last statement, not the expected type.
                    expectedType = if (origin == IrStatementOrigin.FOR_LOOP || expectedType?.isUnit == true) unitType else null
                )
            }
            is FirUnitExpression -> expression.convertWithOffsets { _, endOffset ->
                IrGetObjectValueImpl(
                    endOffset, endOffset, builtins.unitType, this.builtins.unitClass
                )
            }
            else -> {
                when (val unwrappedExpression = expression.unwrapArgument()) {
                    is FirCallableReferenceAccess -> convertCallableReferenceAccess(unwrappedExpression, isDelegate)
                    is FirCollectionLiteral -> convertToArrayLiteral(unwrappedExpression, expectedType)
                    else -> expression.accept(this, null) as IrExpression
                }
            }
        }.let {
            if (expectedType != null) {
                it.prepareExpressionForGivenExpectedType(expression, expectedType = expectedType, forReceiver = false)
            } else {
                it
            }
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

        fun IrExpression.unwrapTypeOperators(): IrExpression {
            return when (this) {
                is IrTypeOperatorCall -> argument.unwrapTypeOperators()
                else -> this
            }
        }

        irReceiver.unwrapTypeOperators().let {
            if (it is IrValueAccessExpression && receiver != selector.explicitReceiver) it.origin = IrStatementOrigin.IMPLICIT_ARGUMENT
        }

        if (receiver is FirSuperReceiverExpression || receiver is FirResolvedQualifier) return irReceiver

        return irReceiver.prepareExpressionForGivenExpectedType(
            expression = receiver,
            expectedType = selector.expectedReceiverType(receiver)
                ?: errorWithAttachment("Cannot determine expected receiver type") {
                    withFirEntry("selector", selector)
                    withFirEntry("receiver", receiver)
                },
            forReceiver = true
        )
    }

    private fun FirQualifiedAccessExpression.expectedReceiverType(
        receiver: FirExpression,
    ): ConeKotlinType? {
        val calleeReference = calleeReference
        if (calleeReference.isError()) return ConeErrorType(calleeReference.diagnostic)

        val referencedDeclaration = calleeReference.toResolvedCallableSymbol()?.unwrapCallRepresentative()?.fir
        if (referencedDeclaration?.origin == FirDeclarationOrigin.DynamicScope) return ConeDynamicType.create(session)

        // When calling an inner class constructor through a typealias, the extension receiver is actually the dispatch receiver
        // because, of course, it is.
        val realDispatchReceiver = if (isConstructorCallOnTypealiasWithInnerRhs()) extensionReceiver else dispatchReceiver

        return when (receiver) {
            realDispatchReceiver -> {
                val dispatchReceiverType = referencedDeclaration?.dispatchReceiverType ?: return null
                when (dispatchReceiverType) {
                    is ConeClassLikeType -> dispatchReceiverType.replaceArgumentsWithStarProjections()
                    // Intersection overrides can have intersection types as dispatch receivers
                    is ConeIntersectionType -> dispatchReceiverType.mapTypes { (it as ConeClassLikeType).replaceArgumentsWithStarProjections() }
                    else -> null
                }
            }
            extensionReceiver -> {
                val extensionReceiverType = referencedDeclaration?.receiverParameter?.typeRef?.coneType ?: return null
                val substitutor = buildSubstitutorByCalledCallable()
                val substitutedType = substitutor.substituteOrSelf(extensionReceiverType)
                // Frontend may write captured types as type arguments (by design), so we need to approximate receiver type after substitution
                c.session.typeApproximator.approximateToSuperType(
                    substitutedType,
                    TypeApproximatorConfiguration.InternalTypesApproximation
                ) ?: substitutedType
            }
            else -> return null
        }
    }

    internal fun convertToIrBlockBody(block: FirBlock): IrBlockBody {
        return block.convertWithOffsets { startOffset, endOffset ->
            IrFactoryImpl.createBlockBody(
                startOffset, endOffset,
                block.statements.mapNotNull { it.toIrStatement() }
            ).also {
                it.coerceStatementsToUnit(coerceLastExpressionToUnit = true)
            }
        }
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
            ?: operationReceiver.explicitReceiver

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

    private fun FirBlock.convertToIrExpressionOrBlock(
        origin: IrStatementOrigin?,
        expectedType: ConeKotlinType?,
    ): IrExpression {
        val coerceToUnit = expectedType?.isUnit == true

        if (this.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
            tryConvertDynamicIncrementOrDecrementToIr()?.let {
                return it.applyIf(coerceToUnit) {
                    coerceToUnitHandlingSpecialBlocks()
                }
            }
        }

        if (this is FirSingleExpressionBlock) {
            when (val stmt = statement) {
                is FirExpression -> return convertToIrExpression(stmt).applyIf(coerceToUnit) {
                    coerceToUnitHandlingSpecialBlocks()
                }
                !is FirDeclaration -> return (stmt.accept(this@Fir2IrVisitor, null) as IrExpression).applyIf(coerceToUnit) {
                    coerceToUnitHandlingSpecialBlocks()
                }
            }
        }

        if (source?.kind !is KtRealSourceElementKind) {
            val firStatement = statements.singleOrNull()
            if (firStatement is FirExpression && firStatement !is FirBlock) {
                return convertToIrExpression(firStatement).applyIf(coerceToUnit) {
                    coerceToUnitHandlingSpecialBlocks()
                }
            }
        }

        return convertToIrBlock(origin, expectedType)
    }

    private fun FirBlock.convertToIrBlock(
        origin: IrStatementOrigin?,
        expectedType: ConeKotlinType?,
    ): IrExpression {
        val irExpectedType = expectedType?.toIrType()
            ?: (statements.lastOrNull() as? FirExpression)?.resolvedType?.takeUnless { it.isNothing }?.toIrType()
            ?: builtins.unitType

        return source.convertWithOffsets(keywordTokens = null) { startOffset, endOffset ->
            if (origin == IrStatementOrigin.DO_WHILE_LOOP) {
                IrCompositeImpl(
                    startOffset, endOffset, irExpectedType, null,
                    statements.mapNotNull { it.toIrStatement() }
                )
            } else {
                val irStatements = statements.mapNotNull { it.toIrStatement() }
                val singleStatement = irStatements.singleOrNull()
                singleStatement as? IrBlock ?: IrBlockImpl(startOffset, endOffset, irExpectedType, origin, irStatements)
            }
        }.also {
            it.coerceStatementsToUnit(coerceLastExpressionToUnit = false)
        }.applyIf(irExpectedType.isUnit()) {
            coerceToUnitHandlingSpecialBlocks()
        }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Any?): IrElement {
        return errorExpression.convertWithOffsets { startOffset, endOffset ->
            IrErrorExpressionImpl(
                startOffset, endOffset,
                errorExpression.resolvedType.toIrType(),
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

            val irBranches = listOf(
                IrBranchImpl(
                    startOffset, endOffset,
                    IrCallImplWithShape(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = builtins.booleanType,
                        symbol = builtins.eqeqSymbol,
                        typeArgumentsCount = 0,
                        valueArgumentsCount = 2,
                        contextParameterCount = 0,
                        hasDispatchReceiver = false,
                        hasExtensionReceiver = false,
                        origin = IrStatementOrigin.EQEQ,
                    ).apply {
                        arguments[0] = irGetLhsValue()
                        arguments[1] = IrConstImpl.constNull(startOffset, endOffset, builtins.nothingNType)
                    },
                    convertToIrExpression(elvisExpression.rhs, expectedType = elvisExpression.resolvedType)
                ),
                IrElseBranchImpl(
                    IrConstImpl.boolean(startOffset, endOffset, builtins.booleanType, true),
                    irGetLhsValue()
                )
            )

            generateWhen(
                startOffset, endOffset, IrStatementOrigin.ELVIS,
                irLhsVariable, irBranches,
                elvisExpression.resolvedType.toIrType()
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
                    if (isProperlyExhaustive) whenExpression.resolvedType else unitType
                val irBranches = whenExpression.convertWhenBranchesTo(
                    mutableListOf(),
                    whenExpressionType,
                    flattenElse = origin == IrStatementOrigin.IF,
                )
                if (isProperlyExhaustive && whenExpression.branches.none { it.condition is FirElseIfTrueCondition }) {
                    val irResult = IrCallImplWithShape(
                        startOffset, endOffset, builtins.nothingType,
                        builtins.noWhenBranchMatchedExceptionSymbol,
                        typeArgumentsCount = 0,
                        valueArgumentsCount = 0,
                        contextParameterCount = 0,
                        hasDispatchReceiver = false,
                        hasExtensionReceiver = false,
                    )
                    irBranches += IrElseBranchImpl(
                        IrConstImpl.boolean(startOffset, endOffset, builtins.booleanType, true), irResult
                    )
                }
                generateWhen(startOffset, endOffset, origin, subjectVariable, irBranches, whenExpressionType.toIrType())
            }
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
        val subjectExpression = subjectVariable?.initializer
        return when {
            subjectVariable != null && !subjectVariable.isImplicitWhenSubjectVariable -> subjectVariable.accept(this, null) as IrVariable
            subjectExpression != null ->
                conversionScope.scope().createTemporaryVariable(
                    irExpression = with(implicitCastInserter) {
                        // We can't pass the expected type to convertToIrExpression because it will break
                        // compiler/testData/codegen/box/when/stringOptimization/enhancedNullability.kt
                        // See KT-47398.
                        convertToIrExpression(subjectExpression).insertCastForIntersectionTypeOrSelf(
                            argumentType = subjectExpression.resolvedType,
                            expectedType = subjectVariable.returnTypeRef.coneType,
                        )
                    },
                    nameHint = "subject",
                )
            else -> null
        }
    }

    private fun FirWhenBranch.toIrWhenBranch(whenExpressionType: ConeKotlinType): IrBranch {
        return convertWithOffsets { startOffset, endOffset ->
            val condition = condition
            val irResult = convertToIrExpression(result, expectedType = whenExpressionType)
            if (condition is FirElseIfTrueCondition) {
                IrElseBranchImpl(IrConstImpl.boolean(irResult.startOffset, irResult.endOffset, builtins.booleanType, true), irResult)
            } else {
                IrBranchImpl(
                    startOffset = startOffset,
                    endOffset = if (irResult.endOffset < 0) endOffset else irResult.endOffset,
                    condition = convertToIrExpression(condition),
                    result = irResult
                )
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
                    doWhileLoop.block.convertToIrExpressionOrBlock(origin, expectedType = unitType)
                }
                condition = convertToIrExpression(doWhileLoop.condition)
                loopMap.remove(doWhileLoop)
            }
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
                                ?: error("Unexpected shape of for loop body: missing body statements: ${whileLoop.render()}")

                            val (destructuredLoopVariables, realStatements) = loopBodyStatements.drop(1).partition {
                                it is FirProperty && it.initializer?.source?.kind is KtFakeSourceElementKind.DestructuringInitializer
                            }
                            val firBlock = realStatements.singleOrNull() as? FirBlock
                                ?: error("Unexpected shape of for loop body: must be single real loop statement, but got ${realStatements.size}. Loop: ${whileLoop.render()}")

                            val irStatements = buildList {
                                val isUnnamedLocalVar = firLoopVarStmt is FirProperty
                                        && firLoopVarStmt.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                                val convertedForLoopVar = when {
                                    isUnnamedLocalVar -> conversionScope.scope().createTemporaryVariable(
                                        convertToIrExpression(firLoopVarStmt.initializer!!),
                                        nameHint = "forLoopVariable",
                                    )
                                    else -> firLoopVarStmt.toIrStatement()
                                }
                                addIfNotNull(convertedForLoopVar)
                                destructuredLoopVariables.forEach { addIfNotNull(it.toIrStatement()) }
                                if (firBlock !is FirEmptyExpressionBlock) {
                                    add(firBlock.convertToIrExpressionOrBlock(origin = null, unitType))
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
                        firLoopBody.convertToIrExpressionOrBlock(null, unitType)
                    }
                }
                loopMap.remove(whileLoop)
            }
        }
    }

    private fun FirJump<FirLoop>.convertJumpWithOffsets(
        f: (startOffset: Int, endOffset: Int, irLoop: IrLoop, label: String?) -> IrBreakContinue,
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
            val expression = convertToIrExpression(throwExpression.exception, expectedType = session.builtinTypes.throwableType.coneType)
            IrThrowImpl(startOffset, endOffset, builtins.nothingType, expression)
        }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: Any?): IrElement {
        // Always generate a block for try, catch and finally blocks. When leaving the finally block in the debugger
        // for both Java and Kotlin there is a step on the end brace. For that to happen we need the block with
        // that line number for the finally block.
        return tryExpression.convertWithOffsets { startOffset, endOffset ->
            IrTryImpl(
                startOffset, endOffset, tryExpression.resolvedType.toIrType(),
                tryExpression.tryBlock
                    .convertToIrBlock(origin = null, expectedType = tryExpression.tryBlock.resolvedType)
                    .prepareExpressionForGivenExpectedType(expression = tryExpression.tryBlock, expectedType = tryExpression.resolvedType, forReceiver = false),
                tryExpression.catches.map { convertCatch(it, tryExpression.resolvedType) },
                tryExpression.finallyBlock?.convertToIrBlock(origin = null, expectedType = unitType)
            )
        }
    }

    private fun convertCatch(firCatch: FirCatch, expectedType: ConeKotlinType): IrCatch {
        return firCatch.convertWithOffsets { startOffset, endOffset ->
            val catchParameter = declarationStorage.createAndCacheIrVariable(
                firCatch.parameter, conversionScope.parentFromStack(), IrDeclarationOrigin.CATCH_PARAMETER
            )
            IrCatchImpl(
                startOffset, endOffset, catchParameter,
                firCatch.block
                    .convertToIrBlock(origin = null, expectedType = firCatch.block.resolvedType)
                    .prepareExpressionForGivenExpectedType(expression = firCatch.block, expectedType = expectedType, forReceiver = false)
            )
        }
    }

    override fun visitCatch(catch: FirCatch, data: Any?): IrElement = shouldNotBeCalled()

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
                if (argument is IrConst && argument.kind == IrConstKind.String) {
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
            val irTypeOperand = typeOperatorCall.conversionTypeRef.toIrType()
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
            IrCallImplWithShape(
                startOffset, endOffset,
                checkNotNullCall.resolvedType.toIrType(),
                builtins.checkNotNullSymbol,
                typeArgumentsCount = 1,
                valueArgumentsCount = 1,
                contextParameterCount = 0,
                hasDispatchReceiver = false,
                hasExtensionReceiver = false,
                origin = IrStatementOrigin.EXCLEXCL
            ).apply {
                typeArguments[0] = checkNotNullCall.argument.resolvedType.toIrType().makeNotNull()
                arguments[0] =convertToIrExpression(checkNotNullCall.argument)
            }
        }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Any?): IrElement = whileAnalysing(session, getClassCall) {
        val argument = getClassCall.argument
        val irType = getClassCall.resolvedType.toIrType()
        val irClassType =
            if (argument is FirClassReferenceExpression) {
                argument.classTypeRef.toIrType()
            } else {
                argument.resolvedType.toIrType()
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

    private fun ConeClassLikeType?.toIrClassSymbol(): IrClassSymbol? {
        return this?.lookupTag?.toClassSymbol()?.let {
            classifierStorage.getIrClassSymbol(it)
        }
    }

    private fun convertToArrayLiteral(
        arrayLiteral: FirCollectionLiteral,
        // This argument is used for a corner case with deserialized empty array literals
        // These array literals normally have a type of Array<Any>,
        // so FIR2IR should instead use a type of corresponding property
        // See also KT-62598
        expectedType: ConeKotlinType?,
    ): IrVararg {
        return arrayLiteral.convertWithOffsets { startOffset, endOffset ->
            val arrayType = (expectedType ?: arrayLiteral.resolvedType).toIrType()
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

    override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression, data: Any?): IrElement {
        return booleanOperatorExpression.convertWithOffsets<IrElement> { startOffset, endOffset ->
            val leftOperand = booleanOperatorExpression.leftOperand.accept(this, data) as IrExpression
            val rightOperand = booleanOperatorExpression.rightOperand.accept(this, data) as IrExpression
            if (leftOperand.type is IrDynamicType) {
                IrDynamicOperatorExpressionImpl(
                    startOffset,
                    endOffset,
                    builtins.booleanType,
                    booleanOperatorExpression.kind.toIrDynamicOperator(),
                ).apply {
                    receiver = leftOperand
                    arguments.add(rightOperand)
                }
            } else when (booleanOperatorExpression.kind) {
                LogicOperationKind.AND -> {
                    IrWhenImpl(startOffset, endOffset, builtins.booleanType, IrStatementOrigin.ANDAND).apply {
                        branches.add(IrBranchImpl(leftOperand, rightOperand))
                        branches.add(elseBranch(constFalse(rightOperand.startOffset, rightOperand.endOffset)))
                    }
                }
                LogicOperationKind.OR -> {
                    IrWhenImpl(startOffset, endOffset, builtins.booleanType, IrStatementOrigin.OROR).apply {
                        branches.add(IrBranchImpl(leftOperand, constTrue(leftOperand.startOffset, leftOperand.endOffset)))
                        branches.add(elseBranch(rightOperand))
                    }
                }
            }
        }
    }

    internal fun isGetClassOfUnresolvedTypeInAnnotation(expression: FirExpression): Boolean =
    // In kapt mode, skip `Unresolved::class` in annotation arguments, because it cannot be handled by IrInterpreter,
        // and because this replicates K1 behavior (see `ConstantExpressionEvaluatorVisitor.visitClassLiteralExpression`).
        configuration.skipBodies && annotationMode &&
                expression is FirGetClassCall && expression.argument.resolvedType is ConeErrorType
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
