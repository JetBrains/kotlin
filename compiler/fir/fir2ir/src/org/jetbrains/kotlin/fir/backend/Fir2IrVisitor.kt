/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.ClassMemberGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.isIteratorNext
import org.jetbrains.kotlin.fir.resolve.transformers.IntegerLiteralTypeApproximationTransformer
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperator
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import java.util.*

class Fir2IrVisitor(
    private val converter: Fir2IrConverter,
    private val components: Fir2IrComponents,
    fakeOverrideMode: FakeOverrideMode
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, Any?>(), IrGeneratorContextInterface {
    companion object {
        private val NEGATED_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY)

        private val UNARY_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.EXCL)
    }

    private val integerApproximator = IntegerLiteralTypeApproximationTransformer(session.firSymbolProvider, session.inferenceContext)

    private val conversionScope = Fir2IrConversionScope()

    private val memberGenerator = ClassMemberGenerator(components, this, conversionScope, fakeOverrideMode)

    private val callGenerator = CallAndReferenceGenerator(components, this, conversionScope)

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

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
        declarationStorage.enterScope(irEnumEntry.descriptor)
        classifierStorage.putEnumEntryClassInScope(enumEntry, correspondingClass)
        converter.processAnonymousObjectMembers(enumEntry.initializer as FirAnonymousObject, correspondingClass)
        conversionScope.withParent(correspondingClass) {
            memberGenerator.convertClassContent(correspondingClass, enumEntry.initializer as FirAnonymousObject)
            irEnumEntry.initializerExpression = IrExpressionBodyImpl(
                IrEnumConstructorCallImpl(
                    startOffset, endOffset, enumEntry.returnTypeRef.toIrType(),
                    correspondingClass.constructors.first().symbol
                )
            )
        }
        declarationStorage.leaveScope(irEnumEntry.descriptor)
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
        declarationStorage.enterScope(irAnonymousInitializer.descriptor)
        irAnonymousInitializer.body = convertToIrBlockBody(anonymousInitializer.body!!)
        declarationStorage.leaveScope(irAnonymousInitializer.descriptor)
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
            val descriptor = irTarget.descriptor
            IrReturnImpl(
                startOffset, endOffset, irBuiltIns.nothingType,
                when (descriptor) {
                    is ClassConstructorDescriptor -> symbolTable.referenceConstructor(descriptor)
                    else -> symbolTable.referenceSimpleFunction(descriptor)
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
        val irReturnType = varargArgumentsExpression.typeRef.toIrType()
        return IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irReturnType,
                            varargArgumentsExpression.varargElementType.toIrType(),
                            varargArgumentsExpression.arguments.map { arg ->
                                convertToIrExpression(arg).run {
                                    if (arg is FirSpreadArgumentExpression) IrSpreadElementImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, this)
                                    else this
                                }
                            })
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?): IrExpression {
        val convertibleCall = if (functionCall.toResolvedCallableSymbol()?.fir is FirIntegerOperator) {
            functionCall.copy().transformSingle(integerApproximator, null)
        } else {
            functionCall
        }
        return callGenerator.convertToIrCall(convertibleCall, convertibleCall.typeRef)
    }

    private fun FirFunctionCall.resolvedNamedFunctionSymbol(): FirNamedFunctionSymbol? {
        val calleeReference = (calleeReference as? FirResolvedNamedReference) ?: return null
        return calleeReference.resolvedSymbol as? FirNamedFunctionSymbol
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): IrElement {
        return callGenerator.convertToIrConstructorCall(annotationCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Any?): IrElement {
        return callGenerator.convertToIrCall(qualifiedAccessExpression, qualifiedAccessExpression.typeRef)
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Any?): IrElement {
        val calleeReference = thisReceiverExpression.calleeReference
        val boundSymbol = calleeReference.boundSymbol
        if (calleeReference.labelName == null) {
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
        }
        // TODO handle qualified "this" in instance methods of non-inner classes (inner class cases are handled by InnerClassesLowering)
        return visitQualifiedAccessExpression(thisReceiverExpression, data)
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: Any?): IrElement {
        // Generate the expression with the original type and then cast it to the smart cast type.
        val value = convertToIrExpression(expressionWithSmartcast.originalExpression)
        val castType = expressionWithSmartcast.typeRef.toIrType()
        if (value.type == castType) return value
        return IrTypeOperatorCallImpl(
            value.startOffset,
            value.endOffset,
            castType,
            IrTypeOperator.IMPLICIT_CAST,
            castType,
            value
        )
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Any?): IrElement {
        return callGenerator.convertToIrCallableReference(callableReferenceAccess)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: Any?): IrElement {
        return callGenerator.convertToIrSetCall(variableAssignment)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Any?): IrElement {
        return constExpression.convertWithOffsets { startOffset, endOffset ->
            @Suppress("UNCHECKED_CAST")
            val kind = constExpression.getIrConstKind() as IrConstKind<T>

            @Suppress("UNCHECKED_CAST")
            val value = (constExpression.value as? Long)?.let {
                when (kind) {
                    IrConstKind.Byte -> it.toByte()
                    IrConstKind.Short -> it.toShort()
                    IrConstKind.Int -> it.toInt()
                    IrConstKind.Float -> it.toFloat()
                    IrConstKind.Double -> it.toDouble()
                    else -> it
                }
            } as T ?: constExpression.value
            IrConstImpl(
                startOffset, endOffset,
                constExpression.typeRef.toIrType(),
                kind, value
            )
        }
    }

    // ==================================================================================

    private fun FirStatement.toIrStatement(): IrStatement? {
        if (this is FirTypeAlias) return null
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
        return conversionScope.withSubject(subjectVariable) {
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
                            startOffset, endOffset, irBuiltIns.nothingType, irBuiltIns.noWhenBranchMatchedExceptionSymbol
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
        val lastSubjectVariable = conversionScope.lastSubject()
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

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Any?): IrElement {
        return comparisonExpression.convertWithOffsets { startOffset, endOffset ->
            generateComparisonCall(startOffset, endOffset, comparisonExpression)
        }
    }

    private fun generateComparisonCall(
        startOffset: Int, endOffset: Int,
        comparisonExpression: FirComparisonExpression
    ): IrExpression {
        val operation = comparisonExpression.operation

        fun fallbackToRealCall(): IrExpression {
            val (symbol, origin) = getSymbolAndOriginForComparison(operation, irBuiltIns.intType.classifierOrFail)
            return primitiveOp2(
                startOffset, endOffset,
                symbol!!,
                irBuiltIns.booleanType,
                origin,
                visitFunctionCall(comparisonExpression.compareToCall, null),
                IrConstImpl.int(startOffset, endOffset, irBuiltIns.intType, 0)
            )
        }

        val comparisonInfo = comparisonExpression.inferPrimitiveNumericComparisonInfo() ?: return fallbackToRealCall()
        val comparisonType = comparisonInfo.comparisonType

        // Currently inferPrimitiveNumericComparisonInfo returns null for different primitive values
        // TODO: Support different primitive types as well and fix inferPrimitiveNumericComparisonInfo
        require(comparisonInfo.leftPrimitiveType == comparisonInfo.rightPrimitiveType && comparisonInfo.leftType == comparisonInfo.rightType) {
            "Contract for inferPrimitiveNumericComparisonInfo is violated"
        }

        val simpleType = when ((comparisonType.type as? ConeClassLikeType)?.lookupTag?.classId) {
            StandardClassIds.Long -> irBuiltIns.longType
            StandardClassIds.Int -> irBuiltIns.intType
            StandardClassIds.Float -> irBuiltIns.floatType
            StandardClassIds.Double -> irBuiltIns.doubleType
            StandardClassIds.Char -> irBuiltIns.charType
            else -> return fallbackToRealCall()
        }

        val (symbol, origin) = getSymbolAndOriginForComparison(operation, simpleType.classifierOrFail)

        return primitiveOp2(
            startOffset, endOffset, symbol!!, irBuiltIns.booleanType, origin,
            convertToIrExpression(comparisonExpression.left), convertToIrExpression(comparisonExpression.right)
        )
    }

    private fun getSymbolAndOriginForComparison(
        operation: FirOperation,
        classifier: IrClassifierSymbol
    ): Pair<IrSimpleFunctionSymbol?, IrStatementOriginImpl> {
        return when (operation) {
            FirOperation.LT -> irBuiltIns.lessFunByOperandType[classifier] to IrStatementOrigin.LT
            FirOperation.GT -> irBuiltIns.greaterFunByOperandType[classifier] to IrStatementOrigin.GT
            FirOperation.LT_EQ -> irBuiltIns.lessOrEqualFunByOperandType[classifier] to IrStatementOrigin.LTEQ
            FirOperation.GT_EQ -> irBuiltIns.greaterOrEqualFunByOperandType[classifier] to IrStatementOrigin.GTEQ
            else -> error("Unexpected comparison operation: $operation")
        }
    }

    private fun generateOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        val (type, symbol, origin) = when (operation) {
            FirOperation.EQ -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ)
            FirOperation.NOT_EQ -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EXCLEQ)
            FirOperation.IDENTITY -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EQEQEQ)
            FirOperation.NOT_IDENTITY -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EXCLEQEQ)
            FirOperation.EXCL -> Triple(irBuiltIns.booleanType, irBuiltIns.booleanNotSymbol, IrStatementOrigin.EXCL)
            FirOperation.LT, FirOperation.GT,
            FirOperation.LT_EQ, FirOperation.GT_EQ,
            FirOperation.OTHER, FirOperation.ASSIGN, FirOperation.PLUS_ASSIGN,
            FirOperation.MINUS_ASSIGN, FirOperation.TIMES_ASSIGN,
            FirOperation.DIV_ASSIGN, FirOperation.REM_ASSIGN,
            FirOperation.IS, FirOperation.NOT_IS,
            FirOperation.AS, FirOperation.SAFE_AS
            -> {
                TODO("Should not be here: incompatible operation in FirOperatorCall: $operation")
            }
        }
        val result = if (operation in UNARY_OPERATIONS) {
            primitiveOp1(startOffset, endOffset, symbol, type, origin, convertToIrExpression(arguments[0]))
        } else {
            primitiveOp2(
                startOffset, endOffset, symbol, type, origin,
                convertToIrExpression(arguments[0]), convertToIrExpression(arguments[1])
            )
        }
        if (operation !in NEGATED_OPERATIONS) return result
        return primitiveOp1(startOffset, endOffset, irBuiltIns.booleanNotSymbol, irBuiltIns.booleanType, origin, result)
    }

    override fun visitOperatorCall(operatorCall: FirOperatorCall, data: Any?): IrElement {
        return operatorCall.convertWithOffsets { startOffset, endOffset ->
            generateOperatorCall(startOffset, endOffset, operatorCall.operation, operatorCall.arguments)
        }
    }

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
                IrStatementOrigin.EXCLEXCL
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
