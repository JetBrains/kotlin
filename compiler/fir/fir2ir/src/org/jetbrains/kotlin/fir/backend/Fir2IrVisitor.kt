/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.ClassMemberGenerator
import org.jetbrains.kotlin.fir.backend.generators.OperatorExpressionGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirStubStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.isIteratorNext
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.transformers.IntegerLiteralTypeApproximationTransformer
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperator
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
import org.jetbrains.kotlin.ir.builders.constFalse
import org.jetbrains.kotlin.ir.builders.constTrue
import org.jetbrains.kotlin.ir.builders.elseBranch
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtForExpression

class Fir2IrVisitor(
    private val converter: Fir2IrConverter,
    private val components: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope,
    fakeOverrideMode: FakeOverrideMode
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, Any?>(), IrGeneratorContextInterface {

    private val integerApproximator = IntegerLiteralTypeApproximationTransformer(
        session.firSymbolProvider,
        session.inferenceContext,
        session
    )

    private val callGenerator = CallAndReferenceGenerator(components, this, conversionScope)

    private val memberGenerator = ClassMemberGenerator(components, this, conversionScope, callGenerator, fakeOverrideMode)

    private val operatorGenerator = OperatorExpressionGenerator(components, this, conversionScope)

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T = conversionScope.applyParentFromStackTo(declaration)

    override fun visitElement(element: FirElement, data: Any?): IrElement {
        TODO("Should not be here: ${element.render()}")
    }

    override fun visitFile(file: FirFile, data: Any?): IrFile {
        return conversionScope.withParent(declarationStorage.getIrFile(file)) {
            file.declarations.forEach {
                it.toIrDeclaration()
            }

            annotations = file.annotations.mapNotNull {
                it.accept(this@Fir2IrVisitor, data) as? IrConstructorCall
            }

            (this as IrFileImpl).metadata = FirMetadataSource.File(file, components.session)
        }
    }

    private fun FirDeclaration.toIrDeclaration(): IrDeclaration? {
        if (this is FirTypeAlias) return null
        return accept(this@Fir2IrVisitor, null) as IrDeclaration
    }

    // ==================================================================================

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?): IrElement {
        val irEnumEntry = classifierStorage.getCachedIrEnumEntry(enumEntry)!!
        val correspondingClass = irEnumEntry.correspondingClass ?: return irEnumEntry
        declarationStorage.enterScope(irEnumEntry)
        classifierStorage.putEnumEntryClassInScope(enumEntry, correspondingClass)
        converter.processAnonymousObjectMembers(enumEntry.initializer as FirAnonymousObject, correspondingClass)
        conversionScope.withParent(correspondingClass) {
            memberGenerator.convertClassContent(correspondingClass, enumEntry.initializer as FirAnonymousObject)
            val constructor = correspondingClass.constructors.first()
            irEnumEntry.initializerExpression = IrExpressionBodyImpl(
                IrEnumConstructorCallImpl(
                    startOffset, endOffset, enumEntry.returnTypeRef.toIrType(),
                    constructor.symbol,
                    typeArgumentsCount = constructor.typeParameters.size,
                    valueArgumentsCount = constructor.valueParameters.size
                )
            )
        }
        declarationStorage.leaveScope(irEnumEntry)
        return irEnumEntry
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?): IrElement {
        if (regularClass.visibility == Visibilities.LOCAL) {
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
            memberGenerator.convertClassContent(irClass, regularClass)
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): IrElement {
        val irParent = conversionScope.parentFromStack()
        // NB: for implicit types it is possible that anonymous object is already cached
        val irAnonymousObject = classifierStorage.getCachedIrClass(anonymousObject)?.apply { this.parent = irParent }
            ?: classifierStorage.createIrAnonymousObject(anonymousObject, irParent = irParent)
        converter.processAnonymousObjectMembers(anonymousObject, irAnonymousObject)
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
            memberGenerator.convertFunctionContent(irConstructor, constructor)
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
        val irFunction = if (simpleFunction.visibility == Visibilities.LOCAL) {
            val irParent = conversionScope.parent()
            declarationStorage.createIrFunction(simpleFunction, irParent)
        } else {
            declarationStorage.getCachedIrFunction(simpleFunction)!!
        }
        return conversionScope.withFunction(irFunction) {
            memberGenerator.convertFunctionContent(irFunction, simpleFunction)
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): IrElement {
        return anonymousFunction.convertWithOffsets { startOffset, endOffset ->
            val irFunction = declarationStorage.createIrFunction(anonymousFunction, conversionScope.parent())
            conversionScope.withFunction(irFunction) {
                memberGenerator.convertFunctionContent(irFunction, anonymousFunction)
            }

            val type = anonymousFunction.typeRef.toIrType()

            IrFunctionExpressionImpl(startOffset, endOffset, type, irFunction, IrStatementOrigin.LAMBDA)
        }
    }

    private fun visitLocalVariable(variable: FirProperty): IrElement {
        assert(variable.isLocal)
        val initializer = variable.initializer
        val isNextVariable = initializer is FirFunctionCall &&
                initializer.resolvedNamedFunctionSymbol()?.callableId?.isIteratorNext() == true &&
                variable.source.psi?.parent is KtForExpression
        val irVariable = declarationStorage.createIrVariable(
            variable, conversionScope.parentFromStack(), if (isNextVariable) IrDeclarationOrigin.FOR_LOOP_VARIABLE else null
        )
        if (initializer != null) {
            irVariable.initializer = convertToIrExpression(initializer)
        }
        return irVariable
    }

    override fun visitProperty(property: FirProperty, data: Any?): IrElement {
        if (property.isLocal) return visitLocalVariable(property)
        val irProperty = declarationStorage.getCachedIrProperty(property)!!
        return conversionScope.withProperty(irProperty) {
            memberGenerator.convertPropertyContent(irProperty, property)
        }
    }

    // ==================================================================================

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Any?): IrElement {
        val irTarget = conversionScope.returnTarget(returnExpression)
        return returnExpression.convertWithOffsets { startOffset, endOffset ->
            val result = returnExpression.result
            IrReturnImpl(
                startOffset, endOffset, irBuiltIns.nothingType,
                when (irTarget) {
                    is IrConstructor -> irTarget.symbol
                    is IrSimpleFunction -> irTarget.symbol
                    else -> throw AssertionError("Should not be here: $irTarget")
                },
                convertToIrExpression(result)
            )
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

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?): IrExpression {
        val convertibleCall = if (functionCall.toResolvedCallableSymbol()?.fir is FirIntegerOperator) {
            functionCall.copy().transformSingle(integerApproximator, null)
        } else {
            functionCall
        }
        val explicitReceiverExpression = convertToIrReceiverExpression(
            functionCall.explicitReceiver, functionCall.calleeReference
        )
        return callGenerator.convertToIrCall(convertibleCall, convertibleCall.typeRef, explicitReceiverExpression)
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: Any?): IrElement {
        val explicitReceiverExpression = convertToIrExpression(safeCallExpression.receiver)

        val (receiverVariable, variableSymbol) = components.createTemporaryVariableForSafeCallConstruction(
            explicitReceiverExpression,
            conversionScope
        )

        return conversionScope.withSafeCallSubject(receiverVariable) {
            val afterNotNullCheck = safeCallExpression.regularQualifiedAccess.accept(this, data) as IrExpression

            val isReceiverNullable = with(components.session.inferenceContext) {
                safeCallExpression.receiver.typeRef.coneTypeSafe<ConeKotlinType>()?.isNullableType() == true
            }

            components.createSafeCallConstruction(
                receiverVariable, variableSymbol, afterNotNullCheck, isReceiverNullable
            )
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

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Any?): IrElement {
        val calleeReference = thisReceiverExpression.calleeReference
        val boundSymbol = calleeReference.boundSymbol
        if (boundSymbol is FirClassSymbol) {
            // Object case
            val firClass = boundSymbol.fir as FirClass
            val irClass = classifierStorage.getCachedIrClass(firClass)!!
            if (firClass is FirAnonymousObject || firClass is FirRegularClass && firClass.classKind == ClassKind.OBJECT) {
                if (irClass != conversionScope.lastClass()) {
                    return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                        IrGetObjectValueImpl(startOffset, endOffset, irClass.defaultType, irClass.symbol)
                    }
                }
            }

            val dispatchReceiver = conversionScope.dispatchReceiverParameter(irClass)
            if (dispatchReceiver != null) {
                return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                    IrGetValueImpl(startOffset, endOffset, dispatchReceiver.type, dispatchReceiver.symbol)
                }
            }
        } else if (boundSymbol is FirCallableSymbol) {
            val receiverSymbol = calleeReference.toSymbol(session, classifierStorage, declarationStorage, conversionScope)
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

    private fun implicitCastOrExpression(original: IrExpression, castType: IrType): IrExpression {
        if (original.type == castType) return original
        return IrTypeOperatorCallImpl(
            original.startOffset,
            original.endOffset,
            castType,
            IrTypeOperator.IMPLICIT_CAST,
            castType,
            original
        )
    }

    private fun implicitCastOrExpression(original: IrExpression, castType: ConeKotlinType): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType())
    }

    private fun implicitCastOrExpression(original: IrExpression, castType: FirTypeRef): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType())
    }

    private fun ConeKotlinType.doesContainReferencedSymbolInScope(
        referencedSymbol: AbstractFirBasedSymbol<*>, name: Name
    ): Boolean {
        val scope = scope(session, components.scopeSession) ?: return false
        var result = false
        val processor = { it: FirCallableSymbol<*> ->
            if (!result && it == referencedSymbol) {
                result = true
            }
        }
        when (referencedSymbol) {
            is FirPropertySymbol -> scope.processPropertiesByName(name, processor)
            is FirFunctionSymbol -> scope.processFunctionsByName(name, processor)
        }
        return result
    }

    private fun convertToImplicitCastExpression(
        expressionWithSmartcast: FirExpressionWithSmartcast, calleeReference: FirReference
    ): IrExpression {
        val value = convertToIrExpression(expressionWithSmartcast.originalExpression)
        val castTypeRef = expressionWithSmartcast.typeRef
        if (calleeReference !is FirResolvedNamedReference) {
            return implicitCastOrExpression(value, castTypeRef)
        }
        val referencedSymbol = calleeReference.resolvedSymbol
        if (referencedSymbol !is FirPropertySymbol && referencedSymbol !is FirFunctionSymbol) {
            return implicitCastOrExpression(value, castTypeRef)
        }

        val originalTypeRef = expressionWithSmartcast.originalType
        if (castTypeRef is FirResolvedTypeRef && originalTypeRef is FirResolvedTypeRef) {
            val castType = castTypeRef.type
            if (castType is ConeIntersectionType) {
                castType.intersectedTypes.forEach {
                    if (it.doesContainReferencedSymbolInScope(referencedSymbol, calleeReference.name)) {
                        return implicitCastOrExpression(value, it)
                    }
                }
            }
        }
        return implicitCastOrExpression(value, castTypeRef.toIrType())
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: Any?): IrElement {
        // Generate the expression with the original type and then cast it to the smart cast type.
        val value = convertToIrExpression(expressionWithSmartcast.originalExpression)
        return implicitCastOrExpression(value, expressionWithSmartcast.typeRef)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Any?): IrElement {
        val explicitReceiverExpression = convertToIrReceiverExpression(
            callableReferenceAccess.explicitReceiver, callableReferenceAccess.calleeReference, callableReferenceMode = true
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

    internal fun convertToIrExpression(expression: FirExpression): IrExpression {
        return when (expression) {
            is FirBlock -> expression.convertToIrExpressionOrBlock()
            is FirUnitExpression -> expression.convertWithOffsets { startOffset, endOffset ->
                IrGetObjectValueImpl(
                    startOffset, endOffset, irBuiltIns.unitType,
                    this.symbolTable.referenceClass(this.irBuiltIns.builtIns.unit)
                )
            }
            else -> expression.accept(this, null) as IrExpression
        }
    }

    private fun convertToIrReceiverExpression(
        expression: FirExpression?,
        calleeReference: FirReference,
        callableReferenceMode: Boolean = false
    ): IrExpression? {
        return when (expression) {
            null -> null
            is FirResolvedQualifier -> callGenerator.convertToGetObject(expression, callableReferenceMode)
            is FirExpressionWithSmartcast -> convertToImplicitCastExpression(expression, calleeReference)
            else -> convertToIrExpression(expression)
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
            IrBlockBodyImpl(
                startOffset, endOffset,
                if (irStatements.isNotEmpty()) {
                    irStatements.filterNotNull().takeIf { it.isNotEmpty() }
                        ?: listOf(IrBlockImpl(startOffset, endOffset, irBuiltIns.unitType, null, emptyList()))
                } else {
                    emptyList()
                }
            )
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
            IrBlockImpl(
                startOffset, endOffset, type, origin,
                mapToIrStatements().filterNotNull()
            )
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

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Any?): IrElement {
        val subjectVariable = generateWhenSubjectVariable(whenExpression)
        val psi = whenExpression.psi
        val origin = when (whenExpression.source?.elementType) {
            KtNodeTypes.WHEN -> IrStatementOrigin.WHEN
            KtNodeTypes.IF -> IrStatementOrigin.IF
            KtNodeTypes.BINARY_EXPRESSION -> when ((psi as? KtBinaryExpression)?.operationToken) {
                KtTokens.ELVIS -> IrStatementOrigin.ELVIS
                KtTokens.OROR -> IrStatementOrigin.OROR
                KtTokens.ANDAND -> IrStatementOrigin.ANDAND
                else -> null
            }
            KtNodeTypes.POSTFIX_EXPRESSION -> IrStatementOrigin.EXCLEXCL
            else -> null
        }
        return conversionScope.withWhenSubject(subjectVariable) {
            whenExpression.convertWithOffsets { startOffset, endOffset ->
                val irWhen = IrWhenImpl(
                    startOffset, endOffset,
                    whenExpression.typeRef.toIrType(),
                    origin
                ).apply {
                    var unconditionalBranchFound = false
                    for (branch in whenExpression.branches) {
                        if (branch.condition !is FirElseIfTrueCondition) {
                            branches += branch.accept(this@Fir2IrVisitor, data) as IrBranch
                        } else {
                            unconditionalBranchFound = true
                            if (branch.result.statements.isNotEmpty()) {
                                branches += branch.accept(this@Fir2IrVisitor, data) as IrBranch
                            }
                        }
                    }
                    if (whenExpression.isExhaustive && !unconditionalBranchFound) {
                        val irResult = IrCallImpl(
                            startOffset, endOffset, irBuiltIns.nothingType,
                            irBuiltIns.noWhenBranchMatchedExceptionSymbol,
                            typeArgumentsCount = 0,
                            valueArgumentsCount = 0
                        )
                        branches += IrElseBranchImpl(
                            IrConstImpl.boolean(startOffset, endOffset, irBuiltIns.booleanType, true), irResult
                        )
                    }
                }
                if (subjectVariable == null) {
                    irWhen
                } else {
                    IrBlockImpl(startOffset, endOffset, irWhen.type, origin, listOf(subjectVariable, irWhen))
                }
            }
        }
    }

    override fun visitWhenBranch(whenBranch: FirWhenBranch, data: Any?): IrElement {
        return whenBranch.convertWithOffsets { startOffset, endOffset ->
            val condition = whenBranch.condition
            val irResult = convertToIrExpression(whenBranch.result)
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
                body = doWhileLoop.block.convertToIrExpressionOrBlock()
                condition = convertToIrExpression(doWhileLoop.condition)
                loopMap.remove(doWhileLoop)
            }
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
                body = whileLoop.block.convertToIrExpressionOrBlock(origin)
                loopMap.remove(whileLoop)
            }
        }
    }

    private fun FirJump<FirLoop>.convertJumpWithOffsets(
        f: (startOffset: Int, endOffset: Int, irLoop: IrLoop) -> IrBreakContinueBase
    ): IrExpression {
        return convertWithOffsets { startOffset, endOffset ->
            val firLoop = target.labeledElement
            val irLoop = loopMap[firLoop]
            if (irLoop == null) {
                IrErrorExpressionImpl(startOffset, endOffset, irBuiltIns.nothingType, "Unbound loop: ${render()}")
            } else {
                f(startOffset, endOffset, irLoop).apply {
                    label = irLoop.label.takeIf { target.labelName != null }
                }
            }
        }
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Any?): IrElement {
        return breakExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop ->
            IrBreakImpl(startOffset, endOffset, irBuiltIns.nothingType, irLoop)
        }
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Any?): IrElement {
        return continueExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop ->
            IrContinueImpl(startOffset, endOffset, irBuiltIns.nothingType, irLoop)
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
            IrCatchImpl(startOffset, endOffset, catchParameter).apply {
                result = catch.block.convertToIrExpressionOrBlock()
            }
        }
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Any?): IrElement =
        operatorGenerator.convertComparisonExpression(comparisonExpression)

    override fun visitOperatorCall(operatorCall: FirOperatorCall, data: Any?): IrElement =
        operatorGenerator.convertOperatorCall(operatorCall)

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
        val irClassType = argument.typeRef.toIrType()
        val irClassReferenceSymbol = when (argument) {
            is FirResolvedReifiedParameterReference -> {
                classifierStorage.getIrTypeParameterSymbol(argument.symbol, ConversionTypeContext.DEFAULT)
            }
            is FirResolvedQualifier -> {
                val symbol = argument.symbol as? FirClassSymbol
                    ?: return getClassCall.convertWithOffsets { startOffset, endOffset ->
                        IrErrorCallExpressionImpl(
                            startOffset, endOffset, irType, "Resolved qualifier ${argument.render()} does not have correct symbol"
                        )
                    }
                classifierStorage.getIrClassSymbol(symbol)
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
