/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.generators.ClassMemberGenerator
import org.jetbrains.kotlin.fir.backend.generators.OperatorExpressionGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirStubStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.isIteratorNext
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.util.OperatorNameConventions

class Fir2IrVisitor(
    private val converter: Fir2IrConverter,
    private val components: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, Any?>(), IrGeneratorContextInterface {

    internal val implicitCastInserter = Fir2IrImplicitCastInserter(components, this)

    private val memberGenerator = ClassMemberGenerator(components, this, conversionScope)

    private val operatorGenerator = OperatorExpressionGenerator(components, this, conversionScope)

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T = conversionScope.applyParentFromStackTo(declaration)

    override fun visitElement(element: FirElement, data: Any?): IrElement {
        TODO("Should not be here: ${element::class} ${element.render()}")
    }

    override fun visitField(field: FirField, data: Any?): IrField {
        if (field.isSynthetic) {
            return declarationStorage.getCachedIrField(field)!!.apply {
                memberGenerator.convertFieldContent(this, field)
            }
        } else {
            throw AssertionError("Unexpected field: ${field.render()}")
        }
    }

    override fun visitFile(file: FirFile, data: Any?): IrFile {
        return conversionScope.withParent(declarationStorage.getIrFile(file)) {
            file.declarations.forEach {
                it.toIrDeclaration()
            }
            annotationGenerator.generate(this, file)
            metadata = FirMetadataSource.File(file)
        }
    }

    private fun FirDeclaration.toIrDeclaration(): IrDeclaration? =
        accept(this@Fir2IrVisitor, null) as IrDeclaration

    // ==================================================================================

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?): IrElement {
        val irTypeAlias = classifierStorage.getCachedTypeAlias(typeAlias)!!
        annotationGenerator.generate(irTypeAlias, typeAlias)
        return irTypeAlias
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?): IrElement {
        val irEnumEntry = classifierStorage.getCachedIrEnumEntry(enumEntry)!!
        annotationGenerator.generate(irEnumEntry, enumEntry)
        val correspondingClass = irEnumEntry.correspondingClass
        val initializer = enumEntry.initializer
        // If the enum entry has its own members, we need to introduce a synthetic class.
        if (correspondingClass != null) {
            declarationStorage.enterScope(irEnumEntry)
            classifierStorage.putEnumEntryClassInScope(enumEntry, correspondingClass)
            val anonymousObject = enumEntry.initializer as FirAnonymousObject
            converter.processAnonymousObjectMembers(anonymousObject, correspondingClass)
            conversionScope.withParent(correspondingClass) {
                conversionScope.withContainingFirClass(anonymousObject) {
                    memberGenerator.convertClassContent(correspondingClass, anonymousObject)
                }
                val constructor = correspondingClass.constructors.first()
                irEnumEntry.initializerExpression = irFactory.createExpressionBody(
                    IrEnumConstructorCallImpl(
                        startOffset, endOffset, enumEntry.returnTypeRef.toIrType(),
                        constructor.symbol,
                        typeArgumentsCount = constructor.typeParameters.size,
                        valueArgumentsCount = constructor.valueParameters.size
                    )
                )
            }
            declarationStorage.leaveScope(irEnumEntry)
        } else if (initializer is FirAnonymousObject) {
            // Otherwise, this is a default-ish enum entry, which doesn't need its own synthetic class.
            // During raw FIR building, we put the delegated constructor call inside an anonymous object.
            val primaryConstructor = initializer.getPrimaryConstructorIfAny()
            val delegatedConstructor = primaryConstructor?.delegatedConstructor
            if (delegatedConstructor != null) {
                with(memberGenerator) {
                    irEnumEntry.initializerExpression = irFactory.createExpressionBody(
                        delegatedConstructor.toIrDelegatingConstructorCall()
                    )
                }
            }
        }
        return irEnumEntry
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?): IrElement {
        if (regularClass.visibility == Visibilities.Local) {
            val irParent = conversionScope.parentFromStack()
            // NB: for implicit types it is possible that local class is already cached
            val irClass = classifierStorage.getCachedIrClass(regularClass)?.apply { this.parent = irParent }
            if (irClass != null) {
                converter.processRegisteredLocalClassAndNestedClasses(regularClass, irClass)
                return conversionScope.withParent(irClass) {
                    memberGenerator.convertClassContent(irClass, regularClass)
                }
            }
            converter.processLocalClassAndNestedClasses(regularClass, irParent)
        }
        val irClass = classifierStorage.getCachedIrClass(regularClass)!!
        return conversionScope.withParent(irClass) {
            conversionScope.withContainingFirClass(regularClass) {
                memberGenerator.convertClassContent(irClass, regularClass)
            }
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): IrElement {
        val irParent = conversionScope.parentFromStack()
        // NB: for implicit types it is possible that anonymous object is already cached
        val irAnonymousObject = classifierStorage.getCachedIrClass(anonymousObject)?.apply { this.parent = irParent }
            ?: classifierStorage.createIrAnonymousObject(anonymousObject, irParent = irParent)
        converter.processAnonymousObjectMembers(anonymousObject, irAnonymousObject)
        conversionScope.withParent(irAnonymousObject) {
            conversionScope.withContainingFirClass(anonymousObject) {
                memberGenerator.convertClassContent(irAnonymousObject, anonymousObject)
            }
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
                        irAnonymousObject.constructors.first().symbol,
                        irAnonymousObject.typeParameters.size,
                        origin = IrStatementOrigin.OBJECT_LITERAL
                    )
                )
            )
        }
    }

    // ==================================================================================

    override fun visitConstructor(constructor: FirConstructor, data: Any?): IrElement {
        val irConstructor = declarationStorage.getCachedIrConstructor(constructor)!!
        return conversionScope.withFunction(irConstructor) {
            memberGenerator.convertFunctionContent(irConstructor, constructor, containingClass = conversionScope.containerFirClass())
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Any?): IrElement {
        val irAnonymousInitializer = declarationStorage.getCachedIrAnonymousInitializer(anonymousInitializer)!!
        declarationStorage.enterScope(irAnonymousInitializer)
        irAnonymousInitializer.body = convertToIrBlockBody(anonymousInitializer.body!!)
        declarationStorage.leaveScope(irAnonymousInitializer)
        return irAnonymousInitializer
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?): IrElement {
        val irFunction = if (simpleFunction.visibility == Visibilities.Local) {
            declarationStorage.createIrFunction(
                simpleFunction, irParent = conversionScope.parent(), isLocal = true
            )
        } else {
            declarationStorage.getCachedIrFunction(simpleFunction)!!
        }
        return conversionScope.withFunction(irFunction) {
            memberGenerator.convertFunctionContent(
                irFunction, simpleFunction, containingClass = conversionScope.containerFirClass()
            )
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): IrElement {
        return anonymousFunction.convertWithOffsets { startOffset, endOffset ->
            val irFunction = declarationStorage.createIrFunction(
                anonymousFunction, irParent = conversionScope.parent(), isLocal = true
            )
            conversionScope.withFunction(irFunction) {
                memberGenerator.convertFunctionContent(irFunction, anonymousFunction, containingClass = null)
            }

            val type = anonymousFunction.typeRef.toIrType()

            IrFunctionExpressionImpl(startOffset, endOffset, type, irFunction, IrStatementOrigin.LAMBDA)
        }
    }

    private fun visitLocalVariable(variable: FirProperty): IrElement {
        assert(variable.isLocal)
        val delegate = variable.delegate
        if (delegate != null) {
            val irProperty = declarationStorage.createIrLocalDelegatedProperty(variable, conversionScope.parentFromStack())
            irProperty.delegate.initializer = convertToIrExpression(delegate)
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
                initializer.resolvedNamedFunctionSymbol()?.callableId?.isIteratorNext() == true &&
                variable.source.psi?.parent is KtForExpression
        val irVariable = declarationStorage.createIrVariable(
            variable, conversionScope.parentFromStack(), if (isNextVariable) IrDeclarationOrigin.FOR_LOOP_VARIABLE else null
        )
        if (initializer != null) {
            irVariable.initializer =
                with(implicitCastInserter) {
                    convertToIrExpression(initializer).cast(initializer, initializer.typeRef, variable.returnTypeRef)
                }
        }
        return irVariable
    }

    override fun visitProperty(property: FirProperty, data: Any?): IrElement {
        if (property.isLocal) return visitLocalVariable(property)
        val irProperty = declarationStorage.getCachedIrProperty(property)!!
        return conversionScope.withProperty(irProperty, property) {
            memberGenerator.convertPropertyContent(irProperty, property, containingClass = conversionScope.containerFirClass())
        }
    }

    // ==================================================================================

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Any?): IrElement {
        val irTarget = conversionScope.returnTarget(returnExpression, declarationStorage)
        return returnExpression.convertWithOffsets { startOffset, endOffset ->
            val result = returnExpression.result
            IrReturnImpl(
                startOffset, endOffset, irBuiltIns.nothingType,
                when (irTarget) {
                    is IrConstructor -> irTarget.symbol
                    is IrSimpleFunction -> irTarget.symbol
                    else -> error("Unknown return target: $irTarget")
                },
                convertToIrExpression(result)
            )
        }.let {
            returnExpression.accept(implicitCastInserter, it)
        }
    }

    override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: Any?): IrElement {
        // TODO: change this temporary hack to something correct
        return convertToIrExpression(wrappedArgumentExpression.expression)
    }

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: Any?): IrElement {
        return varargArgumentsExpression.convertWithOffsets { startOffset, endOffset ->
            IrVarargImpl(
                startOffset,
                endOffset,
                varargArgumentsExpression.typeRef.toIrType(),
                varargArgumentsExpression.varargElementType.toIrType(),
                varargArgumentsExpression.arguments.map { it.convertToIrVarargElement() }
            )
        }
    }

    private fun FirExpression.convertToIrVarargElement(): IrVarargElement =
        if (this is FirSpreadArgumentExpression || this is FirNamedArgumentExpression && this.isSpread) {
            IrSpreadElementImpl(
                source?.startOffset ?: UNDEFINED_OFFSET,
                source?.endOffset ?: UNDEFINED_OFFSET,
                convertToIrExpression(this)
            )
        } else convertToIrExpression(this)

    private fun convertToIrCall(functionCall: FirFunctionCall, annotationMode: Boolean): IrExpression {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            functionCall.explicitReceiver, functionCall.calleeReference
        )
        return callGenerator.convertToIrCall(functionCall, functionCall.typeRef, explicitReceiverExpression, annotationMode)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?): IrExpression {
        return convertToIrCall(functionCall = functionCall, annotationMode = false)
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: Any?): IrElement {
        val explicitReceiverExpression = convertToIrExpression(safeCallExpression.receiver)

        val (receiverVariable, variableSymbol) = components.createTemporaryVariableForSafeCallConstruction(
            explicitReceiverExpression,
            conversionScope
        )

        return conversionScope.withSafeCallSubject(receiverVariable) {
            val afterNotNullCheck = safeCallExpression.regularQualifiedAccess.accept(this, data) as IrExpression
            components.createSafeCallConstruction(receiverVariable, variableSymbol, afterNotNullCheck)
        }
    }

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: Any?): IrElement {
        val lastSubjectVariable = conversionScope.lastSafeCallSubject()
        return checkedSafeCallSubject.convertWithOffsets { startOffset, endOffset ->
            IrGetValueImpl(startOffset, endOffset, lastSubjectVariable.type, lastSubjectVariable.symbol)
        }
    }

    private fun FirFunctionCall.resolvedNamedFunctionSymbol(): FirNamedFunctionSymbol? {
        val calleeReference = (calleeReference as? FirResolvedNamedReference) ?: return null
        return calleeReference.resolvedSymbol as? FirNamedFunctionSymbol
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): IrElement {
        return callGenerator.convertToIrConstructorCall(annotationCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Any?): IrElement {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            qualifiedAccessExpression.explicitReceiver, qualifiedAccessExpression.calleeReference
        )
        return callGenerator.convertToIrCall(qualifiedAccessExpression, qualifiedAccessExpression.typeRef, explicitReceiverExpression)
    }

    // Note that this mimics psi2ir [StatementGenerator#isThisForClassPhysicallyAvailable].
    private fun isThisForClassPhysicallyAvailable(irClass: IrClass): Boolean {
        var lastClass = conversionScope.lastClass()
        while (lastClass != null) {
            if (irClass == lastClass) return true
            if (!lastClass.isInner) return false
            lastClass = lastClass.parentClassOrNull
        }
        return false
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Any?): IrElement {
        val calleeReference = thisReceiverExpression.calleeReference
        val boundSymbol = calleeReference.boundSymbol
        if (boundSymbol is FirClassSymbol) {
            // Object case
            val firClass = boundSymbol.fir as FirClass
            val irClass = classifierStorage.getCachedIrClass(firClass)!!
            // NB: IR generates anonymous objects as classes, not singleton objects
            if (firClass is FirRegularClass && firClass.classKind == ClassKind.OBJECT && !isThisForClassPhysicallyAvailable(irClass)) {
                return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                    IrGetObjectValueImpl(startOffset, endOffset, irClass.defaultType, irClass.symbol)
                }
            }

            val dispatchReceiver = conversionScope.dispatchReceiverParameter(irClass)
            if (dispatchReceiver != null) {
                return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                    IrGetValueImpl(startOffset, endOffset, dispatchReceiver.type, dispatchReceiver.symbol)
                }
            }
        } else if (boundSymbol is FirCallableSymbol) {
            val receiverSymbol = calleeReference.toSymbolForCall(session, classifierStorage, declarationStorage, conversionScope)
            val receiver = (receiverSymbol?.owner as? IrSimpleFunction)?.extensionReceiverParameter
            if (receiver != null) {
                return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                    IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
                }
            }
        }
        // TODO handle qualified "this" in instance methods of non-inner classes (inner class cases are handled by InnerClassesLowering)
        return visitQualifiedAccessExpression(thisReceiverExpression, data)
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: Any?): IrElement {
        // Generate the expression with the original type and then cast it to the smart cast type.
        val value = convertToIrExpression(expressionWithSmartcast.originalExpression)
        return implicitCastInserter.visitExpressionWithSmartcast(expressionWithSmartcast, value)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Any?): IrElement {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            callableReferenceAccess.explicitReceiver, callableReferenceAccess.calleeReference, callableReferenceAccess
        )
        return callGenerator.convertToIrCallableReference(callableReferenceAccess, explicitReceiverExpression)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: Any?): IrElement {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            variableAssignment.explicitReceiver, variableAssignment.calleeReference
        )
        return callGenerator.convertToIrSetCall(variableAssignment, explicitReceiverExpression)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Any?): IrElement =
        constExpression.toIrConst(constExpression.typeRef.toIrType())

    // ==================================================================================

    private fun FirStatement.toIrStatement(): IrStatement? {
        if (this is FirTypeAlias) return null
        if (this == FirStubStatement) return null
        if (this is FirUnitExpression) return convertToIrExpression(this)
        if (this is FirBlock) return convertToIrExpression(this)
        return accept(this@Fir2IrVisitor, null) as IrStatement
    }

    internal fun convertToIrExpression(expression: FirExpression, annotationMode: Boolean = false): IrExpression {
        return when (expression) {
            is FirBlock -> expression.convertToIrExpressionOrBlock()
            is FirUnitExpression -> expression.convertWithOffsets { startOffset, endOffset ->
                IrGetObjectValueImpl(
                    startOffset, endOffset, irBuiltIns.unitType,
                    this.symbolTable.referenceClass(this.irBuiltIns.builtIns.unit)
                )
            }
            else -> {
                val unwrappedExpression = if (expression is FirWrappedArgumentExpression) {
                    expression.expression
                } else {
                    expression
                }
                if (annotationMode && unwrappedExpression is FirFunctionCall) {
                    convertToIrCall(unwrappedExpression, annotationMode)
                } else {
                    expression.accept(this, null) as IrExpression
                }
            }
        }.let {
            expression.accept(implicitCastInserter, it) as IrExpression
        }
    }

    internal fun convertToIrReceiverExpression(
        expression: FirExpression?,
        calleeReference: FirReference,
        callableReferenceAccess: FirCallableReferenceAccess? = null
    ): IrExpression? {
        return when (expression) {
            null -> return null
            is FirResolvedQualifier -> callGenerator.convertToGetObject(expression, callableReferenceAccess)
            is FirFunctionCall, is FirThisReceiverExpression, is FirCallableReferenceAccess, is FirExpressionWithSmartcast ->
                convertToIrExpression(expression)
            else -> if (expression is FirQualifiedAccessExpression && expression.explicitReceiver == null) {
                val variableAsFunctionMode = calleeReference is FirResolvedNamedReference &&
                        calleeReference.name != OperatorNameConventions.INVOKE &&
                        (calleeReference.resolvedSymbol as? FirCallableSymbol)?.callableId?.callableName == OperatorNameConventions.INVOKE
                callGenerator.convertToIrCall(
                    expression, expression.typeRef, explicitReceiverExpression = null,
                    variableAsFunctionMode = variableAsFunctionMode
                )
            } else {
                convertToIrExpression(expression)
            }
        }?.run {
            if (expression is FirQualifiedAccessExpression && expression.calleeReference is FirSuperReference) return@run this

            implicitCastInserter.implicitCastFromDispatchReceiver(this, expression.typeRef, calleeReference)
        }
    }

    private fun FirBlock.mapToIrStatements(): List<IrStatement?> {
        val irRawStatements = statements.map { it.toIrStatement() }
        val result = mutableListOf<IrStatement?>()
        var missNext = false
        for ((index, irRawStatement) in irRawStatements.withIndex()) {
            if (missNext) {
                missNext = false
                continue
            } else if (irRawStatement is IrVariable && irRawStatement.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR) {
                missNext = true
                val irNextStatement = irRawStatements[index + 1]!!
                result += IrBlockImpl(
                    irRawStatement.startOffset, irNextStatement.endOffset,
                    (irNextStatement as IrExpression).type, IrStatementOrigin.FOR_LOOP,
                    listOf(irRawStatement, irNextStatement)
                )
            } else {
                result += irRawStatement
            }
        }
        return result
    }

    internal fun convertToIrBlockBody(block: FirBlock): IrBlockBody {
        return block.convertWithOffsets { startOffset, endOffset ->
            val irStatements = block.mapToIrStatements()
            irFactory.createBlockBody(
                startOffset, endOffset,
                if (irStatements.isNotEmpty()) {
                    irStatements.filterNotNull().takeIf { it.isNotEmpty() }
                        ?: listOf(IrBlockImpl(startOffset, endOffset, irBuiltIns.unitType, null, emptyList()))
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

    private fun FirBlock.convertToIrExpressionOrBlock(origin: IrStatementOrigin? = null): IrExpression {
        if (statements.size == 1) {
            val firStatement = statements.single()
            if (firStatement is FirExpression) {
                return convertToIrExpression(firStatement)
            }
        }
        val type =
            (statements.lastOrNull() as? FirExpression)?.typeRef?.toIrType() ?: irBuiltIns.unitType
        return convertWithOffsets { startOffset, endOffset ->
            if (origin == IrStatementOrigin.DO_WHILE_LOOP) {
                IrCompositeImpl(
                    startOffset, endOffset, type, origin,
                    mapToIrStatements().filterNotNull()
                )
            } else {
                IrBlockImpl(
                    startOffset, endOffset, type, origin,
                    mapToIrStatements().filterNotNull()
                )
            }
        }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Any?): IrElement {
        return errorExpression.convertWithOffsets { startOffset, endOffset ->
            IrErrorExpressionImpl(
                startOffset, endOffset,
                errorExpression.typeRef.toIrType(),
                errorExpression.diagnostic.reason
            )
        }
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: Any?): IrElement {
        val firLhsVariable = buildProperty {
            source = elvisExpression.source
            session = this@Fir2IrVisitor.session
            origin = FirDeclarationOrigin.Source
            returnTypeRef = elvisExpression.lhs.typeRef
            name = Name.special("<elvis>")
            initializer = elvisExpression.lhs
            symbol = FirPropertySymbol(name)
            isVar = false
            isLocal = true
            status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
        }
        val irLhsVariable = firLhsVariable.accept(this, null) as IrVariable
        return elvisExpression.convertWithOffsets { startOffset, endOffset ->
            fun irGetLhsValue(): IrGetValue =
                IrGetValueImpl(startOffset, endOffset, irLhsVariable.type, irLhsVariable.symbol)

            val originalType = firLhsVariable.returnTypeRef.coneType
            val notNullType = originalType.withNullability(ConeNullability.NOT_NULL)
            val irBranches = listOf(
                IrBranchImpl(
                    startOffset, endOffset, primitiveOp2(
                        startOffset, endOffset, irBuiltIns.eqeqSymbol,
                        irBuiltIns.booleanType, IrStatementOrigin.EQEQ,
                        irGetLhsValue(),
                        IrConstImpl.constNull(startOffset, endOffset, irBuiltIns.nothingNType)
                    ),
                    convertToIrExpression(elvisExpression.rhs)
                ),
                IrElseBranchImpl(
                    IrConstImpl.boolean(startOffset, endOffset, irBuiltIns.booleanType, true),
                    if (notNullType == originalType) {
                        irGetLhsValue()
                    } else {
                        implicitCastInserter.implicitCastOrExpression(
                            irGetLhsValue(),
                            firLhsVariable.returnTypeRef.resolvedTypeFromPrototype(notNullType).toIrType()
                        )
                    }
                )
            )

            generateWhen(
                startOffset, endOffset, IrStatementOrigin.ELVIS,
                irLhsVariable, irBranches,
                elvisExpression.typeRef.toIrType()
            )
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Any?): IrElement {
        val subjectVariable = generateWhenSubjectVariable(whenExpression)
        val psi = whenExpression.psi
        val origin = when (whenExpression.source?.elementType) {
            KtNodeTypes.WHEN -> IrStatementOrigin.WHEN
            KtNodeTypes.IF -> IrStatementOrigin.IF
            KtNodeTypes.BINARY_EXPRESSION -> when ((psi as? KtBinaryExpression)?.operationToken) {
                KtTokens.OROR -> IrStatementOrigin.OROR
                KtTokens.ANDAND -> IrStatementOrigin.ANDAND
                else -> null
            }
            KtNodeTypes.POSTFIX_EXPRESSION -> IrStatementOrigin.EXCLEXCL
            else -> null
        }
        return conversionScope.withWhenSubject(subjectVariable) {
            whenExpression.convertWithOffsets { startOffset, endOffset ->
                // If the constant true branch has empty body, it won't be converted. Thus, the entire `when` expression is effectively _not_
                // exhaustive anymore. In that case, coerce the return type of `when` expression to Unit as per the backend expectation.
                val irBranches = whenExpression.branches.mapNotNullTo(mutableListOf()) { branch ->
                    branch.takeIf {
                        it.condition !is FirElseIfTrueCondition || it.result.statements.isNotEmpty()
                    }?.toIrWhenBranch(whenExpression.typeRef)
                }
                if (whenExpression.isExhaustive && whenExpression.branches.none { it.condition is FirElseIfTrueCondition }) {
                    val irResult = IrCallImpl(
                        startOffset, endOffset, irBuiltIns.nothingType,
                        irBuiltIns.noWhenBranchMatchedExceptionSymbol,
                        typeArgumentsCount = 0,
                        valueArgumentsCount = 0
                    )
                    irBranches += IrElseBranchImpl(
                        IrConstImpl.boolean(startOffset, endOffset, irBuiltIns.booleanType, true), irResult
                    )
                }
                generateWhen(
                    startOffset, endOffset, origin,
                    subjectVariable, irBranches,
                    if (whenExpression.isExhaustive && whenExpression.branches.none {
                            it.condition is FirElseIfTrueCondition && it.result.statements.isEmpty()
                        }
                    ) whenExpression.typeRef.toIrType() else irBuiltIns.unitType
                )
            }
        }.also {
            whenExpression.accept(implicitCastInserter, it)
        }
    }

    private fun generateWhen(
        startOffset: Int,
        endOffset: Int,
        origin: IrStatementOrigin?,
        subjectVariable: IrVariable?,
        branches: List<IrBranch>,
        resultType: IrType
    ): IrExpression {
        val irWhen = IrWhenImpl(startOffset, endOffset, resultType, origin, branches)
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
            subjectExpression != null -> {
                applyParentFromStackTo(declarationStorage.declareTemporaryVariable(convertToIrExpression(subjectExpression), "subject"))
            }
            else -> null
        }
    }

    private fun FirWhenBranch.toIrWhenBranch(whenExpressionType: FirTypeRef): IrBranch {
        return convertWithOffsets { startOffset, endOffset ->
            val condition = condition
            val irResult = with(implicitCastInserter) {
                convertToIrExpression(result).cast(result, result.typeRef, whenExpressionType)
            }
            if (condition is FirElseIfTrueCondition) {
                IrElseBranchImpl(IrConstImpl.boolean(irResult.startOffset, irResult.endOffset, irBuiltIns.booleanType, true), irResult)
            } else {
                IrBranchImpl(startOffset, endOffset, convertToIrExpression(condition), irResult)
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
        return doWhileLoop.convertWithOffsets { startOffset, endOffset ->
            IrDoWhileLoopImpl(
                startOffset, endOffset, irBuiltIns.unitType,
                IrStatementOrigin.DO_WHILE_LOOP
            ).apply {
                loopMap[doWhileLoop] = this
                label = doWhileLoop.label?.name
                body = doWhileLoop.block.convertToIrExpressionOrBlock(origin)
                condition = convertToIrExpression(doWhileLoop.condition)
                loopMap.remove(doWhileLoop)
            }
        }.also {
            doWhileLoop.accept(implicitCastInserter, it)
        }
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: Any?): IrElement {
        return whileLoop.convertWithOffsets { startOffset, endOffset ->
            val origin = if (whileLoop.source?.elementType == KtNodeTypes.FOR) IrStatementOrigin.FOR_LOOP_INNER_WHILE
            else IrStatementOrigin.WHILE_LOOP
            IrWhileLoopImpl(startOffset, endOffset, irBuiltIns.unitType, origin).apply {
                loopMap[whileLoop] = this
                label = whileLoop.label?.name
                condition = convertToIrExpression(whileLoop.condition)
                // NB: here we have strange origin logic, made to be compatible with FE 1.0
                body = whileLoop.block.convertToIrExpressionOrBlock(origin.takeIf { it != IrStatementOrigin.WHILE_LOOP })
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
                IrErrorExpressionImpl(startOffset, endOffset, irBuiltIns.nothingType, "Unbound loop: ${render()}")
            } else {
                f(startOffset, endOffset, irLoop, irLoop.label.takeIf { target.labelName != null })
            }
        }
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Any?): IrElement {
        return breakExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop, label ->
            IrBreakImpl(startOffset, endOffset, irBuiltIns.nothingType, irLoop).apply {
                this.label = label
            }
        }
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Any?): IrElement {
        return continueExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop, label ->
            IrContinueImpl(startOffset, endOffset, irBuiltIns.nothingType, irLoop).apply {
                this.label = label
            }
        }
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Any?): IrElement {
        return throwExpression.convertWithOffsets { startOffset, endOffset ->
            IrThrowImpl(startOffset, endOffset, irBuiltIns.nothingType, convertToIrExpression(throwExpression.exception))
        }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: Any?): IrElement {
        return tryExpression.convertWithOffsets { startOffset, endOffset ->
            IrTryImpl(
                startOffset, endOffset, tryExpression.typeRef.toIrType(),
                tryExpression.tryBlock.convertToIrExpressionOrBlock(),
                tryExpression.catches.map { it.accept(this, data) as IrCatch },
                tryExpression.finallyBlock?.convertToIrExpressionOrBlock()
            )
        }
    }

    override fun visitCatch(catch: FirCatch, data: Any?): IrElement {
        return catch.convertWithOffsets { startOffset, endOffset ->
            val catchParameter = declarationStorage.createIrVariable(catch.parameter, conversionScope.parentFromStack())
            IrCatchImpl(startOffset, endOffset, catchParameter, catch.block.convertToIrExpressionOrBlock())
        }
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Any?): IrElement =
        operatorGenerator.convertComparisonExpression(comparisonExpression)

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Any?): IrElement {
        return stringConcatenationCall.convertWithOffsets { startOffset, endOffset ->
            IrStringConcatenationImpl(
                startOffset, endOffset, irBuiltIns.stringType,
                stringConcatenationCall.arguments.map { convertToIrExpression(it) }
            )
        }
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): IrElement {
        return typeOperatorCall.convertWithOffsets { startOffset, endOffset ->
            val irTypeOperand = typeOperatorCall.conversionTypeRef.toIrType()
            val (irType, irTypeOperator) = when (typeOperatorCall.operation) {
                FirOperation.IS -> irBuiltIns.booleanType to IrTypeOperator.INSTANCEOF
                FirOperation.NOT_IS -> irBuiltIns.booleanType to IrTypeOperator.NOT_INSTANCEOF
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
        return operatorGenerator.convertEqualityOperatorCall(equalityOperatorCall)
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: Any?): IrElement {
        return checkNotNullCall.convertWithOffsets { startOffset, endOffset ->
            IrCallImpl(
                startOffset, endOffset,
                checkNotNullCall.typeRef.toIrType(),
                irBuiltIns.checkNotNullSymbol,
                typeArgumentsCount = 1,
                valueArgumentsCount = 1,
                origin = IrStatementOrigin.EXCLEXCL
            ).apply {
                putTypeArgument(0, checkNotNullCall.argument.typeRef.toIrType().makeNotNull())
                putValueArgument(0, convertToIrExpression(checkNotNullCall.argument))
            }
        }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Any?): IrElement {
        val argument = getClassCall.argument
        val irType = getClassCall.typeRef.toIrType()
        val irClassType =
            if (argument is FirClassReferenceExpression) {
                argument.classTypeRef.toIrType()
            } else {
                argument.typeRef.toIrType()
            }
        val irClassReferenceSymbol = when (argument) {
            is FirResolvedReifiedParameterReference -> {
                classifierStorage.getIrTypeParameterSymbol(argument.symbol, ConversionTypeContext.DEFAULT)
            }
            is FirResolvedQualifier -> {
                when (val symbol = argument.symbol) {
                    is FirClassSymbol -> {
                        classifierStorage.getIrClassSymbol(symbol)
                    }
                    is FirTypeAliasSymbol -> {
                        symbol.fir.expandedConeType.toIrClassSymbol()
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

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: Any?): IrElement {
        return arrayOfCall.convertWithOffsets { startOffset, endOffset ->
            lateinit var elementType: IrType
            lateinit var arrayType: IrType
            // Resolved arrayOf call will have resolved type. FirArrayOfCall from collection literal won't.
            if (arrayOfCall.typeRef is FirResolvedTypeRef) {
                arrayType = arrayOfCall.typeRef.toIrType()
                elementType = arrayType.getArrayElementType(irBuiltIns)
            } else {
                // TODO: The element type should be the least upper bound of all arguments' types, e.g., ["4", 2u, 0.42f] => Array<Any>
                //   Currently, the type of elements in array literals still has integer literal type, which shouldn't be at this stage.
                //   elementType = arrayOfCall.arguments.map { it.typeRef.toIrType() }.commonSupertype(irBuiltIns)
                elementType = arrayOfCall.arguments.firstOrNull()?.typeRef?.toIrType() ?: createErrorType()
                arrayType = elementType.toArrayOrPrimitiveArrayType(irBuiltIns)
            }
            IrVarargImpl(
                startOffset, endOffset,
                type = arrayType,
                varargElementType = elementType,
                elements = arrayOfCall.arguments.map { it.convertToIrVarargElement() }
            )
        }
    }

    override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: Any?): IrElement {
        return augmentedArraySetCall.convertWithOffsets { startOffset, endOffset ->
            IrErrorCallExpressionImpl(
                startOffset, endOffset, irBuiltIns.unitType,
                "FirArraySetCall (resolve isn't supported yet)"
            )
        }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Any?): IrElement {
        return callGenerator.convertToGetObject(resolvedQualifier)
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Any?): IrElement {
        return binaryLogicExpression.convertWithOffsets<IrElement> { startOffset, endOffset ->
            val leftOperand = binaryLogicExpression.leftOperand.accept(this, data) as IrExpression
            val rightOperand = binaryLogicExpression.rightOperand.accept(this, data) as IrExpression
            when (binaryLogicExpression.kind) {
                LogicOperationKind.AND -> {
                    IrIfThenElseImpl(startOffset, endOffset, irBuiltIns.booleanType, IrStatementOrigin.ANDAND).apply {
                        branches.add(IrBranchImpl(leftOperand, rightOperand))
                        branches.add(elseBranch(constFalse(rightOperand.startOffset, rightOperand.endOffset)))
                    }
                }
                LogicOperationKind.OR -> {
                    IrIfThenElseImpl(startOffset, endOffset, irBuiltIns.booleanType, IrStatementOrigin.OROR).apply {
                        branches.add(IrBranchImpl(leftOperand, constTrue(leftOperand.startOffset, leftOperand.endOffset)))
                        branches.add(elseBranch(rightOperand))
                    }
                }
            }
        }
    }
}
