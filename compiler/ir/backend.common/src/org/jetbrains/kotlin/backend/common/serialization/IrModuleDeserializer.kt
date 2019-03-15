/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.IrDeserializer
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr.IrStatement.*
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr.IrOperation.OperationCase.*
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr.IrConst.ValueCase.*
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr.IrDeclarator.DeclaratorCase.*
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr.IrType.KindCase.*
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr.IrVarargElement.VarargElementCase
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr.IrTypeArgument.KindCase.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

// TODO: This code still has some uses of descriptors:
// 1. We use descriptors as keys for symbolTable -- probably symbol table related code should be refactored out from
// the deserializer.
// 2. Properties use descriptors but not symbols -- that causes lots of assymmetry all around.
// 3. Declarations are provided with wrapped descriptors. That is probably a legitimate descriptor use.

abstract class IrModuleDeserializer(
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable
) : IrDeserializer {

    abstract fun deserializeIrSymbol(proto: KotlinIr.IrSymbol): IrSymbol
    abstract fun deserializeIrType(proto: KotlinIr.IrTypeIndex): IrType
    abstract fun deserializeDescriptorReference(proto: KotlinIr.DescriptorReference): DeclarationDescriptor
    abstract fun deserializeString(proto: KotlinIr.String): String
    abstract fun deserializeLoopHeader(loopIndex: Int, loopBuilder: () -> IrLoopBase): IrLoopBase

    private fun deserializeName(proto: KotlinIr.String): Name {
        val name = deserializeString(proto)
        return Name.guessByFirstCharacter(name)
    }

    private fun deserializeTypeArguments(proto: KotlinIr.TypeArguments): List<IrType> {
        logger.log { "### deserializeTypeArguments" }
        val result = mutableListOf<IrType>()
        proto.typeArgumentList.forEach { typeProto ->
            val type = deserializeIrType(typeProto)
            result.add(type)
            logger.log { "$type" }
        }
        return result
    }

    fun deserializeIrTypeVariance(variance: KotlinIr.IrTypeVariance) = when (variance) {
        KotlinIr.IrTypeVariance.IN -> Variance.IN_VARIANCE
        KotlinIr.IrTypeVariance.OUT -> Variance.OUT_VARIANCE
        KotlinIr.IrTypeVariance.INV -> Variance.INVARIANT
    }

    fun deserializeIrTypeArgument(proto: KotlinIr.IrTypeArgument) = when (proto.kindCase) {
        STAR -> IrStarProjectionImpl
        TYPE -> makeTypeProjection(
            deserializeIrType(proto.type.type), deserializeIrTypeVariance(proto.type.variance)
        )
        else -> TODO("Unexpected projection kind")

    }

    fun deserializeAnnotations(annotations: KotlinIr.Annotations): List<IrCall> {
        return annotations.annotationList.map {
            deserializeCall(it, 0, 0, builtIns.unitType) // TODO: need a proper deserialization here
        }
    }

    open fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean): IrSimpleType? = null

    fun deserializeSimpleType(proto: KotlinIr.IrSimpleType): IrSimpleType {
        val symbol = deserializeIrSymbol(proto.classifier) as? IrClassifierSymbol
            ?: error("could not convert sym to ClassifierSymbol")
        logger.log { "deserializeSimpleType: symbol=$symbol" }

        val result = getPrimitiveTypeOrNull(symbol, proto.hasQuestionMark) ?: run {
            val arguments = proto.argumentList.map { deserializeIrTypeArgument(it) }
            val annotations = deserializeAnnotations(proto.annotations)
            IrSimpleTypeImpl(
                null,
                symbol,
                proto.hasQuestionMark,
                arguments,
                annotations
            )
        }
        logger.log { "ir_type = $result; render = ${result.render()}" }
        return result

    }

    fun deserializeDynamicType(proto: KotlinIr.IrDynamicType): IrDynamicType {
        val annotations = deserializeAnnotations(proto.annotations)
        return IrDynamicTypeImpl(null, annotations, Variance.INVARIANT)
    }

    fun deserializeErrorType(proto: KotlinIr.IrErrorType): IrErrorType {
        val annotations = deserializeAnnotations(proto.annotations)
        return IrErrorTypeImpl(null, annotations, Variance.INVARIANT)
    }

    fun deserializeIrTypeData(proto: KotlinIr.IrType): IrType {
        return when (proto.kindCase) {
            SIMPLE -> deserializeSimpleType(proto.simple)
            DYNAMIC -> deserializeDynamicType(proto.dynamic)
            ERROR -> deserializeErrorType(proto.error)
            else -> TODO("Unexpected IrType kind: ${proto.kindCase}")
        }
    }

    /* -------------------------------------------------------------- */

    private fun deserializeBlockBody(
        proto: KotlinIr.IrBlockBody,
        start: Int, end: Int
    ): IrBlockBody {

        val statements = mutableListOf<IrStatement>()

        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        return IrBlockBodyImpl(start, end, statements)
    }

    private fun deserializeBranch(proto: KotlinIr.IrBranch, start: Int, end: Int): IrBranch {

        val condition = deserializeExpression(proto.condition)
        val result = deserializeExpression(proto.result)

        return IrBranchImpl(start, end, condition, result)
    }

    private fun deserializeCatch(proto: KotlinIr.IrCatch, start: Int, end: Int): IrCatch {
        val catchParameter =
            deserializeDeclaration(proto.catchParameter, null) as IrVariable // TODO: we need a proper parent here
        val result = deserializeExpression(proto.result)

        val catch = IrCatchImpl(start, end, catchParameter, result)
        return catch
    }

    private fun deserializeSyntheticBody(proto: KotlinIr.IrSyntheticBody, start: Int, end: Int): IrSyntheticBody {
        val kind = when (proto.kind) {
            KotlinIr.IrSyntheticBodyKind.ENUM_VALUES -> IrSyntheticBodyKind.ENUM_VALUES
            KotlinIr.IrSyntheticBodyKind.ENUM_VALUEOF -> IrSyntheticBodyKind.ENUM_VALUEOF
        }
        return IrSyntheticBodyImpl(start, end, kind)
    }

    private fun deserializeStatement(proto: KotlinIr.IrStatement): IrElement {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val element = when (proto.statementCase) {
            StatementCase.BLOCK_BODY //proto.hasBlockBody()
            -> deserializeBlockBody(proto.blockBody, start, end)
            StatementCase.BRANCH //proto.hasBranch()
            -> deserializeBranch(proto.branch, start, end)
            StatementCase.CATCH //proto.hasCatch()
            -> deserializeCatch(proto.catch, start, end)
            StatementCase.DECLARATION // proto.hasDeclaration()
            -> deserializeDeclaration(proto.declaration, null) // TODO: we need a proper parent here.
            StatementCase.EXPRESSION // proto.hasExpression()
            -> deserializeExpression(proto.expression)
            StatementCase.SYNTHETIC_BODY // proto.hasSyntheticBody()
            -> deserializeSyntheticBody(proto.syntheticBody, start, end)
            else
            -> TODO("Statement deserialization not implemented: ${proto.statementCase}")
        }

        logger.log { "### Deserialized statement: ${ir2string(element)}" }

        return element
    }

    private fun deserializeBlock(proto: KotlinIr.IrBlock, start: Int, end: Int, type: IrType): IrBlock {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        val isLambdaOrigin = if (proto.isLambdaOrigin) IrStatementOrigin.LAMBDA else null

        return IrBlockImpl(start, end, type, isLambdaOrigin, statements)
    }

    private fun deserializeMemberAccessCommon(access: IrMemberAccessExpression, proto: KotlinIr.MemberAccessCommon) {

        proto.valueArgumentList.mapIndexed { i, arg ->
            if (arg.hasExpression()) {
                val expr = deserializeExpression(arg.expression)
                access.putValueArgument(i, expr)
            }
        }

        deserializeTypeArguments(proto.typeArguments).forEachIndexed { index, type ->
            access.putTypeArgument(index, type)
        }

        if (proto.hasDispatchReceiver()) {
            access.dispatchReceiver = deserializeExpression(proto.dispatchReceiver)
        }
        if (proto.hasExtensionReceiver()) {
            access.extensionReceiver = deserializeExpression(proto.extensionReceiver)
        }
    }

    private fun deserializeClassReference(
        proto: KotlinIr.IrClassReference,
        start: Int,
        end: Int,
        type: IrType
    ): IrClassReference {
        val symbol = deserializeIrSymbol(proto.classSymbol) as IrClassifierSymbol
        val classType = deserializeIrType(proto.classType)
        /** TODO: [createClassifierSymbolForClassReference] is internal function */
        return IrClassReferenceImpl(start, end, type, symbol, classType)
    }

    private fun deserializeCall(proto: KotlinIr.IrCall, start: Int, end: Int, type: IrType): IrCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrFunctionSymbol

        val superSymbol = if (proto.hasSuper()) {
            deserializeIrSymbol(proto.`super`) as IrClassSymbol
        } else null

        val call: IrCall = when (proto.kind) {
            KotlinIr.IrCall.Primitive.NOT_PRIMITIVE ->
                // TODO: implement the last three args here.
                IrCallImpl(
                    start, end, type,
                    symbol, symbol.descriptor,
                    proto.memberAccess.typeArguments.typeArgumentCount,
                    proto.memberAccess.valueArgumentList.size,
                    null, superSymbol
                )
            KotlinIr.IrCall.Primitive.NULLARY ->
                IrNullaryPrimitiveImpl(start, end, type, null, symbol)
            KotlinIr.IrCall.Primitive.UNARY ->
                IrUnaryPrimitiveImpl(start, end, type, null, symbol)
            KotlinIr.IrCall.Primitive.BINARY ->
                IrBinaryPrimitiveImpl(start, end, type, null, symbol)
            else -> TODO("Unexpected primitive IrCall.")
        }
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    private fun deserializeComposite(proto: KotlinIr.IrComposite, start: Int, end: Int, type: IrType): IrComposite {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }
        return IrCompositeImpl(start, end, type, null, statements)
    }

    private fun deserializeDelegatingConstructorCall(
        proto: KotlinIr.IrDelegatingConstructorCall,
        start: Int,
        end: Int
    ): IrDelegatingConstructorCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrConstructorSymbol
        val call = IrDelegatingConstructorCallImpl(
            start,
            end,
            builtIns.unitType,
            symbol,
            symbol.descriptor,
            proto.memberAccess.typeArguments.typeArgumentCount,
            proto.memberAccess.valueArgumentList.size
        )

        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }


    fun deserializeEnumConstructorCall(
        proto: KotlinIr.IrEnumConstructorCall,
        start: Int,
        end: Int,
        type: IrType
    ): IrEnumConstructorCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrConstructorSymbol
        val call = IrEnumConstructorCallImpl(
            start,
            end,
            type,
            symbol,
            proto.memberAccess.typeArguments.typeArgumentList.size,
            proto.memberAccess.valueArgumentList.size
        )
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }


    private fun deserializeFunctionReference(
        proto: KotlinIr.IrFunctionReference,
        start: Int, end: Int, type: IrType
    ): IrFunctionReference {

        val symbol = deserializeIrSymbol(proto.symbol) as IrFunctionSymbol
        val callable = IrFunctionReferenceImpl(
            start,
            end,
            type,
            symbol,
            symbol.descriptor,
            proto.memberAccess.typeArguments.typeArgumentCount,
            proto.memberAccess.valueArgumentCount,
            null
        )
        deserializeMemberAccessCommon(callable, proto.memberAccess)

        return callable
    }

    private fun deserializeGetClass(proto: KotlinIr.IrGetClass, start: Int, end: Int, type: IrType): IrGetClass {
        val argument = deserializeExpression(proto.argument)
        return IrGetClassImpl(start, end, type, argument)
    }

    private fun deserializeGetField(proto: KotlinIr.IrGetField, start: Int, end: Int, type: IrType): IrGetField {
        val access = proto.fieldAccess
        val symbol = deserializeIrSymbol(access.symbol) as IrFieldSymbol
        val superQualifier = if (access.hasSuper()) {
            deserializeIrSymbol(access.symbol) as IrClassSymbol
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null

        return IrGetFieldImpl(start, end, symbol, type, receiver, null, superQualifier)
    }

    private fun deserializeGetValue(proto: KotlinIr.IrGetValue, start: Int, end: Int, type: IrType): IrGetValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrValueSymbol
        // TODO: origin!
        return IrGetValueImpl(start, end, type, symbol, null)
    }

    private fun deserializeGetEnumValue(proto: KotlinIr.IrGetEnumValue, start: Int, end: Int, type: IrType): IrGetEnumValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrEnumEntrySymbol
        return IrGetEnumValueImpl(start, end, type, symbol)
    }

    private fun deserializeGetObject(proto: KotlinIr.IrGetObject, start: Int, end: Int, type: IrType): IrGetObjectValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrGetObjectValueImpl(start, end, type, symbol)
    }

    private fun deserializeInstanceInitializerCall(
        proto: KotlinIr.IrInstanceInitializerCall,
        start: Int,
        end: Int
    ): IrInstanceInitializerCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrInstanceInitializerCallImpl(start, end, symbol, builtIns.unitType)
    }

    private val getterToPropertyDescriptorMap = mutableMapOf<IrSimpleFunctionSymbol, WrappedPropertyDescriptor>()

    private fun deserializePropertyReference(
        proto: KotlinIr.IrPropertyReference,
        start: Int, end: Int, type: IrType
    ): IrPropertyReference {

        val field = if (proto.hasField()) deserializeIrSymbol(proto.field) as IrFieldSymbol else null
        val getter = if (proto.hasGetter()) deserializeIrSymbol(proto.getter) as IrSimpleFunctionSymbol else null
        val setter = if (proto.hasSetter()) deserializeIrSymbol(proto.setter) as IrSimpleFunctionSymbol else null
        val descriptor =
        if (proto.hasDescriptorReference())
            deserializeDescriptorReference(proto.descriptorReference) as PropertyDescriptor
        else
            field?.descriptor as? WrappedPropertyDescriptor // If field's descriptor coincides with property's.
                ?: getterToPropertyDescriptorMap.getOrPut(getter!!) { WrappedPropertyDescriptor() }

        val callable = IrPropertyReferenceImpl(
            start, end, type,
            descriptor,
            proto.memberAccess.typeArguments.typeArgumentCount,
            field,
            getter,
            setter,
            null
        )
        deserializeMemberAccessCommon(callable, proto.memberAccess)
        return callable
    }

    private fun deserializeReturn(proto: KotlinIr.IrReturn, start: Int, end: Int, type: IrType): IrReturn {
        val symbol = deserializeIrSymbol(proto.returnTarget) as IrReturnTargetSymbol
        val value = deserializeExpression(proto.value)
        return IrReturnImpl(start, end, builtIns.nothingType, symbol, value)
    }

    private fun deserializeSetField(proto: KotlinIr.IrSetField, start: Int, end: Int): IrSetField {
        val access = proto.fieldAccess
        val symbol = deserializeIrSymbol(access.symbol) as IrFieldSymbol
        val superQualifier = if (access.hasSuper()) {
            deserializeIrSymbol(access.symbol) as IrClassSymbol
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null
        val value = deserializeExpression(proto.value)

        return IrSetFieldImpl(start, end, symbol, receiver, value, builtIns.unitType, null, superQualifier)
    }

    private fun deserializeSetVariable(proto: KotlinIr.IrSetVariable, start: Int, end: Int): IrSetVariable {
        val symbol = deserializeIrSymbol(proto.symbol) as IrVariableSymbol
        val value = deserializeExpression(proto.value)
        return IrSetVariableImpl(start, end, builtIns.unitType, symbol, value, null)
    }

    private fun deserializeSpreadElement(proto: KotlinIr.IrSpreadElement): IrSpreadElement {
        val expression = deserializeExpression(proto.expression)
        return IrSpreadElementImpl(proto.coordinates.startOffset, proto.coordinates.endOffset, expression)
    }

    private fun deserializeStringConcat(
        proto: KotlinIr.IrStringConcat,
        start: Int,
        end: Int,
        type: IrType
    ): IrStringConcatenation {
        val argumentProtos = proto.argumentList
        val arguments = mutableListOf<IrExpression>()

        argumentProtos.forEach {
            arguments.add(deserializeExpression(it))
        }
        return IrStringConcatenationImpl(start, end, type, arguments)
    }

    private fun deserializeThrow(proto: KotlinIr.IrThrow, start: Int, end: Int, type: IrType): IrThrowImpl {
        return IrThrowImpl(start, end, builtIns.nothingType, deserializeExpression(proto.value))
    }

    private fun deserializeTry(proto: KotlinIr.IrTry, start: Int, end: Int, type: IrType): IrTryImpl {
        val result = deserializeExpression(proto.result)
        val catches = mutableListOf<IrCatch>()
        proto.catchList.forEach {
            catches.add(deserializeStatement(it) as IrCatch)
        }
        val finallyExpression =
            if (proto.hasFinally()) deserializeExpression(proto.getFinally()) else null
        return IrTryImpl(start, end, type, result, catches, finallyExpression)
    }

    private fun deserializeTypeOperator(operator: KotlinIr.IrTypeOperator) = when (operator) {
        KotlinIr.IrTypeOperator.CAST
        -> IrTypeOperator.CAST
        KotlinIr.IrTypeOperator.IMPLICIT_CAST
        -> IrTypeOperator.IMPLICIT_CAST
        KotlinIr.IrTypeOperator.IMPLICIT_NOTNULL
        -> IrTypeOperator.IMPLICIT_NOTNULL
        KotlinIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        -> IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        KotlinIr.IrTypeOperator.IMPLICIT_INTEGER_COERCION
        -> IrTypeOperator.IMPLICIT_INTEGER_COERCION
        KotlinIr.IrTypeOperator.SAFE_CAST
        -> IrTypeOperator.SAFE_CAST
        KotlinIr.IrTypeOperator.INSTANCEOF
        -> IrTypeOperator.INSTANCEOF
        KotlinIr.IrTypeOperator.NOT_INSTANCEOF
        -> IrTypeOperator.NOT_INSTANCEOF
        KotlinIr.IrTypeOperator.SAM_CONVERSION
        -> IrTypeOperator.SAM_CONVERSION
    }

    private fun deserializeTypeOp(proto: KotlinIr.IrTypeOp, start: Int, end: Int, type: IrType): IrTypeOperatorCall {
        val operator = deserializeTypeOperator(proto.operator)
        val operand = deserializeIrType(proto.operand)//.brokenIr
        val argument = deserializeExpression(proto.argument)
        return IrTypeOperatorCallImpl(start, end, type, operator, operand).apply {
            this.argument = argument
            this.typeOperandClassifier = operand.classifierOrFail
        }
    }

    private fun deserializeVararg(proto: KotlinIr.IrVararg, start: Int, end: Int, type: IrType): IrVararg {
        val elementType = deserializeIrType(proto.elementType)

        val elements = mutableListOf<IrVarargElement>()
        proto.elementList.forEach {
            elements.add(deserializeVarargElement(it))
        }
        return IrVarargImpl(start, end, type, elementType, elements)
    }

    private fun deserializeVarargElement(element: KotlinIr.IrVarargElement): IrVarargElement {
        return when (element.varargElementCase) {
            VarargElementCase.EXPRESSION
            -> deserializeExpression(element.expression)
            VarargElementCase.SPREAD_ELEMENT
            -> deserializeSpreadElement(element.spreadElement)
            else
            -> TODO("Unexpected vararg element")
        }
    }

    private fun deserializeWhen(proto: KotlinIr.IrWhen, start: Int, end: Int, type: IrType): IrWhen {
        val branches = mutableListOf<IrBranch>()

        proto.branchList.forEach {
            branches.add(deserializeStatement(it) as IrBranch)
        }

        // TODO: provide some origin!
        return IrWhenImpl(start, end, type, null, branches)
    }

    private fun deserializeLoop(proto: KotlinIr.Loop, loop: IrLoopBase): IrLoopBase {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val body = if (proto.hasBody()) deserializeExpression(proto.body) else null
        val condition = deserializeExpression(proto.condition)

        loop.label = label
        loop.condition = condition
        loop.body = body

        return loop
    }

    // we create the loop before deserializing the body, so that
    // IrBreak statements have something to put into 'loop' field.
    private fun deserializeDoWhile(proto: KotlinIr.IrDoWhile, start: Int, end: Int, type: IrType) =
            deserializeLoop(proto.loop, deserializeLoopHeader(proto.loop.loopId) { IrDoWhileLoopImpl(start, end, type, null) })

    private fun deserializeWhile(proto: KotlinIr.IrWhile, start: Int, end: Int, type: IrType) =
            deserializeLoop(proto.loop, deserializeLoopHeader(proto.loop.loopId) { IrWhileLoopImpl(start, end, type, null) })

    private fun deserializeDynamicMemberExpression(proto: KotlinIr.IrDynamicMemberExpression, start: Int, end: Int, type: IrType) =
        IrDynamicMemberExpressionImpl(start, end, type, deserializeString(proto.memberName), deserializeExpression(proto.receiver))

    private fun deserializeDynamicOperatorExpression(proto: KotlinIr.IrDynamicOperatorExpression, start: Int, end: Int, type: IrType) =
        IrDynamicOperatorExpressionImpl(start, end, type, deserializeDynamicOperator(proto.operator)).apply {
            receiver = deserializeExpression(proto.receiver)
            proto.argumentList.mapTo(arguments) { deserializeExpression(it) }
        }

    private fun deserializeDynamicOperator(operator: KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator) = when (operator) {
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.UNARY_PLUS -> IrDynamicOperator.UNARY_PLUS
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.UNARY_MINUS -> IrDynamicOperator.UNARY_MINUS

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EXCL -> IrDynamicOperator.EXCL

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.PREFIX_INCREMENT -> IrDynamicOperator.PREFIX_INCREMENT
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.PREFIX_DECREMENT -> IrDynamicOperator.PREFIX_DECREMENT

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.POSTFIX_INCREMENT -> IrDynamicOperator.POSTFIX_INCREMENT
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.POSTFIX_DECREMENT -> IrDynamicOperator.POSTFIX_DECREMENT

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.BINARY_PLUS -> IrDynamicOperator.BINARY_PLUS
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.BINARY_MINUS -> IrDynamicOperator.BINARY_MINUS
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MUL -> IrDynamicOperator.MUL
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.DIV -> IrDynamicOperator.DIV
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MOD -> IrDynamicOperator.MOD

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.GT -> IrDynamicOperator.GT
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.LT -> IrDynamicOperator.LT
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.GE -> IrDynamicOperator.GE
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.LE -> IrDynamicOperator.LE

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EQEQ -> IrDynamicOperator.EQEQ
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EXCLEQ -> IrDynamicOperator.EXCLEQ

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EQEQEQ -> IrDynamicOperator.EQEQEQ
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EXCLEQEQ -> IrDynamicOperator.EXCLEQEQ

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.ANDAND -> IrDynamicOperator.ANDAND
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.OROR -> IrDynamicOperator.OROR

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EQ -> IrDynamicOperator.EQ
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.PLUSEQ -> IrDynamicOperator.PLUSEQ
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MINUSEQ -> IrDynamicOperator.MINUSEQ
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MULEQ -> IrDynamicOperator.MULEQ
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.DIVEQ -> IrDynamicOperator.DIVEQ
        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MODEQ -> IrDynamicOperator.MODEQ

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.ARRAY_ACCESS -> IrDynamicOperator.ARRAY_ACCESS

        KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.INVOKE -> IrDynamicOperator.INVOKE
    }

    private fun deserializeBreak(proto: KotlinIr.IrBreak, start: Int, end: Int, type: IrType): IrBreak {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val loopId = proto.loopId
        val loop = deserializeLoopHeader(loopId) { error("break clause before loop header") }
        val irBreak = IrBreakImpl(start, end, type, loop)
        irBreak.label = label

        return irBreak
    }

    private fun deserializeContinue(proto: KotlinIr.IrContinue, start: Int, end: Int, type: IrType): IrContinue {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val loopId = proto.loopId
        val loop = deserializeLoopHeader(loopId) { error("continue clause before loop header") }
        val irContinue = IrContinueImpl(start, end, type, loop)
        irContinue.label = label

        return irContinue
    }

    private fun deserializeConst(proto: KotlinIr.IrConst, start: Int, end: Int, type: IrType): IrExpression =
        when (proto.valueCase) {
            NULL
            -> IrConstImpl.constNull(start, end, type)
            BOOLEAN
            -> IrConstImpl.boolean(start, end, type, proto.boolean)
            BYTE
            -> IrConstImpl.byte(start, end, type, proto.byte.toByte())
            CHAR
            -> IrConstImpl.char(start, end, type, proto.char.toChar())
            SHORT
            -> IrConstImpl.short(start, end, type, proto.short.toShort())
            INT
            -> IrConstImpl.int(start, end, type, proto.int)
            LONG
            -> IrConstImpl.long(start, end, type, proto.long)
            STRING
            -> IrConstImpl.string(start, end, type, deserializeString(proto.string))
            FLOAT
            -> IrConstImpl.float(start, end, type, proto.float)
            DOUBLE
            -> IrConstImpl.double(start, end, type, proto.double)
            VALUE_NOT_SET
            -> error("Const deserialization error: ${proto.valueCase} ")
        }

    private fun deserializeOperation(proto: KotlinIr.IrOperation, start: Int, end: Int, type: IrType): IrExpression =
        when (proto.operationCase) {
            BLOCK
            -> deserializeBlock(proto.block, start, end, type)
            BREAK
            -> deserializeBreak(proto.`break`, start, end, type)
            CLASS_REFERENCE
            -> deserializeClassReference(proto.classReference, start, end, type)
            CALL
            -> deserializeCall(proto.call, start, end, type)
            COMPOSITE
            -> deserializeComposite(proto.composite, start, end, type)
            CONST
            -> deserializeConst(proto.const, start, end, type)
            CONTINUE
            -> deserializeContinue(proto.`continue`, start, end, type)
            DELEGATING_CONSTRUCTOR_CALL
            -> deserializeDelegatingConstructorCall(proto.delegatingConstructorCall, start, end)
            DO_WHILE
            -> deserializeDoWhile(proto.doWhile, start, end, type)
            ENUM_CONSTRUCTOR_CALL
            -> deserializeEnumConstructorCall(proto.enumConstructorCall, start, end, type)
            FUNCTION_REFERENCE
            -> deserializeFunctionReference(proto.functionReference, start, end, type)
            GET_ENUM_VALUE
            -> deserializeGetEnumValue(proto.getEnumValue, start, end, type)
            GET_CLASS
            -> deserializeGetClass(proto.getClass, start, end, type)
            GET_FIELD
            -> deserializeGetField(proto.getField, start, end, type)
            GET_OBJECT
            -> deserializeGetObject(proto.getObject, start, end, type)
            GET_VALUE
            -> deserializeGetValue(proto.getValue, start, end, type)
            INSTANCE_INITIALIZER_CALL
            -> deserializeInstanceInitializerCall(proto.instanceInitializerCall, start, end)
            PROPERTY_REFERENCE
            -> deserializePropertyReference(proto.propertyReference, start, end, type)
            RETURN
            -> deserializeReturn(proto.`return`, start, end, type)
            SET_FIELD
            -> deserializeSetField(proto.setField, start, end)
            SET_VARIABLE
            -> deserializeSetVariable(proto.setVariable, start, end)
            STRING_CONCAT
            -> deserializeStringConcat(proto.stringConcat, start, end, type)
            THROW
            -> deserializeThrow(proto.`throw`, start, end, type)
            TRY
            -> deserializeTry(proto.`try`, start, end, type)
            TYPE_OP
            -> deserializeTypeOp(proto.typeOp, start, end, type)
            VARARG
            -> deserializeVararg(proto.vararg, start, end, type)
            WHEN
            -> deserializeWhen(proto.`when`, start, end, type)
            WHILE
            -> deserializeWhile(proto.`while`, start, end, type)
            DYNAMIC_MEMBER
            -> deserializeDynamicMemberExpression(proto.dynamicMember, start, end, type)
            DYNAMIC_OPERATOR
            -> deserializeDynamicOperatorExpression(proto.dynamicOperator, start, end, type)
            OPERATION_NOT_SET
            -> error("Expression deserialization not implemented: ${proto.operationCase}")
        }

    private fun deserializeExpression(proto: KotlinIr.IrExpression): IrExpression {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val type = deserializeIrType(proto.type)
        val operation = proto.operation
        val expression = deserializeOperation(operation, start, end, type)

        logger.log { "### Deserialized expression: ${ir2string(expression)} ir_type=$type" }
        return expression
    }

    private fun deserializeIrTypeParameter(
        proto: KotlinIr.IrTypeParameter,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrTypeParameter {
        val symbol = deserializeIrSymbol(proto.symbol) as IrTypeParameterSymbol
        val name = deserializeName(proto.name)
        val variance = deserializeIrTypeVariance(proto.variance)

        val parameter = symbolTable.declareGlobalTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
            symbol.descriptor, { symbol ->
                IrTypeParameterImpl(start, end, origin, symbol, name, proto.index, proto.isReified, variance)
            }
        )

        val superTypes = proto.superTypeList.map { deserializeIrType(it) }
        parameter.superTypes.addAll(superTypes)

        return parameter
    }

    private fun deserializeIrTypeParameterContainer(
        proto: KotlinIr.IrTypeParameterContainer,
        parent: IrDeclarationParent
    ): List<IrTypeParameter> {
        return proto.typeParameterList.map {
            deserializeDeclaration(
                it,
                parent
            ) as IrTypeParameter
        } // TODO: we need proper start, end and origin here?
    }

    private fun deserializeClassKind(kind: KotlinIr.ClassKind) = when (kind) {
        KotlinIr.ClassKind.CLASS -> ClassKind.CLASS
        KotlinIr.ClassKind.INTERFACE -> ClassKind.INTERFACE
        KotlinIr.ClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
        KotlinIr.ClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
        KotlinIr.ClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        KotlinIr.ClassKind.OBJECT -> ClassKind.OBJECT
    }

    private fun deserializeIrValueParameter(
        proto: KotlinIr.IrValueParameter,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrValueParameter {

        val varargElementType = if (proto.hasVarargElementType()) deserializeIrType(proto.varargElementType) else null
        val paramSymbol = deserializeIrSymbol(proto.symbol) as IrValueParameterSymbol
        val parameter =
            IrValueParameterImpl(
                start, end, origin,
                paramSymbol,
                deserializeName(proto.name),
                proto.index,
                deserializeIrType(proto.type),
                varargElementType,
                proto.isCrossinline,
                proto.isNoinline
            ).apply {
                defaultValue = if (proto.hasDefaultValue()) {
                    val expression = deserializeExpression(proto.defaultValue)
                    IrExpressionBodyImpl(expression)
                } else null
            }

        return parameter
    }

    private fun deserializeIrClass(proto: KotlinIr.IrClass, start: Int, end: Int, origin: IrDeclarationOrigin): IrClass {

        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol

        val modality = deserializeModality(proto.modality)
        val clazz = symbolTable.declareClass(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
            symbol.descriptor, modality) {
            IrClassImpl(
                        start, end, origin,
                        it,
                        deserializeName(proto.name),
                        deserializeClassKind(proto.kind),
                        deserializeVisibility(proto.visibility),
                        modality,
                        proto.isCompanion,
                        proto.isInner,
                        proto.isData,
                        proto.isExternal,
                        proto.isInline
            )
        }

        proto.declarationContainer.declarationList.forEach {
            val member = deserializeDeclaration(it, clazz)
            clazz.addMember(member)
            member.parent = clazz
        }

        clazz.thisReceiver = deserializeDeclaration(proto.thisReceiver, clazz) as IrValueParameter

        val typeParameters = deserializeIrTypeParameterContainer(proto.typeParameters, clazz)
        clazz.typeParameters.addAll(typeParameters)

        val superTypes = proto.superTypeList.map { deserializeIrType(it) }
        clazz.superTypes.addAll(superTypes)

        return clazz
    }

    private fun deserializeIrFunctionBase(
        base: KotlinIr.IrFunctionBase,
        function: IrFunctionBase,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ) {

        function.body = if (base.hasBody()) deserializeStatement(base.body) as IrBody else null

        val valueParameters = base.valueParameterList.map { deserializeDeclaration(it, function) as IrValueParameter }
        function.valueParameters.addAll(valueParameters)
        function.dispatchReceiverParameter = if (base.hasDispatchReceiver()) deserializeDeclaration(
            base.dispatchReceiver,
            function
        ) as IrValueParameter else null
        function.extensionReceiverParameter = if (base.hasExtensionReceiver()) deserializeDeclaration(
            base.extensionReceiver,
            function
        ) as IrValueParameter else null
        val typeParameters = deserializeIrTypeParameterContainer(base.typeParameters, function) // TODO
        function.typeParameters.addAll(typeParameters)
    }

    private fun deserializeIrFunction(
        proto: KotlinIr.IrFunction,
        start: Int, end: Int, origin: IrDeclarationOrigin
    ): IrSimpleFunction {

        logger.log { "### deserializing IrFunction ${proto.base.name}" }
        val symbol = deserializeIrSymbol(proto.symbol) as IrSimpleFunctionSymbol

        val function = symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
            symbol.descriptor, {
                IrFunctionImpl(
                    start, end, origin, it,
                    deserializeName(proto.base.name),
                    deserializeVisibility(proto.base.visibility),
                    deserializeModality(proto.modality),
                    deserializeIrType(proto.base.returnType),
                    proto.base.isInline,
                    proto.base.isExternal,
                    proto.isTailrec,
                    proto.isSuspend
                )
            })

        deserializeIrFunctionBase(proto.base, function as IrFunctionBase, start, end, origin)
        val overridden = proto.overriddenList.map { deserializeIrSymbol(it) as IrSimpleFunctionSymbol }
        function.overriddenSymbols.addAll(overridden)

//        function.correspondingProperty = correspondingProperty

        return function
    }

    private fun deserializeIrVariable(
        proto: KotlinIr.IrVariable,
        start: Int, end: Int, origin: IrDeclarationOrigin
    ): IrVariable {

        val initializer = if (proto.hasInitializer()) {
            deserializeExpression(proto.initializer)
        } else null

        val symbol = deserializeIrSymbol(proto.symbol) as IrVariableSymbol
        val type = deserializeIrType(proto.type)

        val variable = IrVariableImpl(
            start,
            end,
            origin,
            symbol,
            deserializeName(proto.name),
            type,
            proto.isVar,
            proto.isConst,
            proto.isLateinit
        )
        variable.initializer = initializer
        return variable
    }

    private fun deserializeIrEnumEntry(
        proto: KotlinIr.IrEnumEntry,
        start: Int, end: Int, origin: IrDeclarationOrigin
    ): IrEnumEntry {
        val symbol = deserializeIrSymbol(proto.symbol) as IrEnumEntrySymbol

        val enumEntry = symbolTable.declareEnumEntry(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            irrelevantOrigin,
            symbol.descriptor
        ) {
            IrEnumEntryImpl(start, end, origin, it, deserializeName(proto.name))
        }

        if (proto.hasCorrespondingClass()) {
            enumEntry.correspondingClass = deserializeDeclaration(proto.correspondingClass, null) as IrClass
        }
        if (proto.hasInitializer()) {
            enumEntry.initializerExpression = deserializeExpression(proto.initializer)
        }

        return enumEntry
    }

    private fun deserializeIrAnonymousInit(
        proto: KotlinIr.IrAnonymousInit,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrAnonymousInitializer {
        val symbol = deserializeIrSymbol(proto.symbol) as IrAnonymousInitializerSymbol
        val initializer = IrAnonymousInitializerImpl(start, end, origin, symbol)
        initializer.body = deserializeBlockBody(proto.body.blockBody, start, end)
        return initializer
    }

    private fun deserializeVisibility(value: KotlinIr.Visibility): Visibility { // TODO: switch to enum
        return when (deserializeString(value.name)) {
            "public" -> Visibilities.PUBLIC
            "private" -> Visibilities.PRIVATE
            "private_to_this" -> Visibilities.PRIVATE_TO_THIS
            "protected" -> Visibilities.PROTECTED
            "internal" -> Visibilities.INTERNAL
            "invisible_fake" -> Visibilities.INVISIBLE_FAKE // TODO: eventually we should not serialize fake overrides, so this will be gone.
            "local" -> Visibilities.LOCAL
            else -> error("Unexpected visibility value: $value")
        }
    }

    private fun deserializeIrConstructor(
        proto: KotlinIr.IrConstructor,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrConstructor {
        val symbol = deserializeIrSymbol(proto.symbol) as IrConstructorSymbol

        val constructor = symbolTable.declareConstructor(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
            symbol.descriptor, {
                IrConstructorImpl(
                    start, end, origin,
                    it,
                    deserializeName(proto.base.name),
                    deserializeVisibility(proto.base.visibility),
                    deserializeIrType(proto.base.returnType),
                    proto.base.isInline,
                    proto.base.isExternal,
                    proto.isPrimary
                )

            })

        deserializeIrFunctionBase(proto.base, constructor as IrFunctionBase, start, end, origin)
        return constructor
    }

    private fun deserializeIrField(proto: KotlinIr.IrField, start: Int, end: Int, origin: IrDeclarationOrigin): IrField {

        val symbol = deserializeIrSymbol(proto.symbol) as IrFieldSymbol
        val type = deserializeIrType(proto.type)
        val field = symbolTable.declareField(UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            irrelevantOrigin,
            symbol.descriptor,
            type,
            { IrFieldImpl(
                    start, end, origin,
                    it,
                    deserializeName(proto.name),
                    type,
                    deserializeVisibility(proto.visibility),
                    proto.isFinal,
                    proto.isExternal,
                    proto.isStatic
                )
            }
        )

        val initializer = if (proto.hasInitializer()) deserializeExpression(proto.initializer) else null
        field.initializer = initializer?.let { IrExpressionBodyImpl(it) }

        return field
    }

    private fun deserializeModality(modality: KotlinIr.ModalityKind) = when (modality) {
        KotlinIr.ModalityKind.OPEN_MODALITY -> Modality.OPEN
        KotlinIr.ModalityKind.SEALED_MODALITY -> Modality.SEALED
        KotlinIr.ModalityKind.FINAL_MODALITY -> Modality.FINAL
        KotlinIr.ModalityKind.ABSTRACT_MODALITY -> Modality.ABSTRACT
    }

    private fun deserializeIrProperty(
        proto: KotlinIr.IrProperty,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrProperty {

        val backingField = if (proto.hasBackingField()) deserializeIrField(proto.backingField, start, end, origin) else null
        val getter = if (proto.hasGetter()) deserializeIrFunction(proto.getter, start, end, origin) else null
        val setter = if (proto.hasSetter()) deserializeIrFunction(proto.setter, start, end, origin) else null

        backingField?.let { (it.descriptor as? WrappedFieldDescriptor)?.bind(it) }
        getter?.let { (it.descriptor as? WrappedSimpleFunctionDescriptor)?.bind(it) }
        setter?.let { (it.descriptor as? WrappedSimpleFunctionDescriptor)?.bind(it) }

        val descriptor =
            if (proto.hasDescriptorReference())
                deserializeDescriptorReference(proto.descriptorReference) as PropertyDescriptor
            else
                backingField?.descriptor as? WrappedPropertyDescriptor // If field's descriptor coincides with property's.
                    ?: getterToPropertyDescriptorMap.getOrPut(getter!!.symbol) { WrappedPropertyDescriptor() }

        val property = IrPropertyImpl(
            start, end, origin,
            descriptor,
            deserializeName(proto.name),
            deserializeVisibility(proto.visibility),
            deserializeModality(proto.modality),
            proto.isVar,
            proto.isConst,
            proto.isLateinit,
            proto.isDelegated,
            proto.isExternal
        )

        symbolTable.referenceProperty(descriptor, { property })

        property.backingField = backingField
        property.getter = getter
        property.setter = setter

        backingField?.let { it.correspondingProperty = property }
        getter?.let { it.correspondingProperty = property }
        setter?.let { it.correspondingProperty = property }

        return property
    }

    private fun deserializeIrTypeAlias(
        proto: KotlinIr.IrTypeAlias,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrDeclaration {
        return IrErrorDeclarationImpl(start, end, WrappedClassDescriptor())
    }

    private val allKnownOrigins =
        IrDeclarationOrigin::class.nestedClasses.toList() + DeclarationFactory.FIELD_FOR_OUTER_THIS::class
    val originIndex = allKnownOrigins.map { it.objectInstance as IrDeclarationOriginImpl }.associateBy { it.name }
    val irrelevantOrigin = object : IrDeclarationOriginImpl("irrelevant") {}
    fun deserializeIrDeclarationOrigin(proto: KotlinIr.IrDeclarationOrigin) = originIndex[deserializeString(proto.custom)]!!

    protected fun deserializeDeclaration(proto: KotlinIr.IrDeclaration, parent: IrDeclarationParent?): IrDeclaration {

        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val origin = deserializeIrDeclarationOrigin(proto.origin)

        val declarator = proto.declarator

        val declaration: IrDeclaration = when (declarator.declaratorCase) {
            IR_ANONYMOUS_INIT
            -> deserializeIrAnonymousInit(declarator.irAnonymousInit, start, end, origin)
            IR_CONSTRUCTOR
            -> deserializeIrConstructor(declarator.irConstructor, start, end, origin)
            IR_FIELD
            -> deserializeIrField(declarator.irField, start, end, origin)
            IR_CLASS
            -> deserializeIrClass(declarator.irClass, start, end, origin)
            IR_FUNCTION
            -> deserializeIrFunction(declarator.irFunction, start, end, origin)
            IR_PROPERTY
            -> deserializeIrProperty(declarator.irProperty, start, end, origin)
            IR_TYPE_ALIAS
            -> deserializeIrTypeAlias(declarator.irTypeAlias, start, end, origin)
            IR_TYPE_PARAMETER
            -> deserializeIrTypeParameter(declarator.irTypeParameter, start, end, origin)
            IR_VARIABLE
            -> deserializeIrVariable(declarator.irVariable, start, end, origin)
            IR_VALUE_PARAMETER
            -> deserializeIrValueParameter(declarator.irValueParameter, start, end, origin)
            IR_ENUM_ENTRY
            -> deserializeIrEnumEntry(declarator.irEnumEntry, start, end, origin)
            DECLARATOR_NOT_SET
            -> error("Declaration deserialization not implemented: ${declarator.declaratorCase}")
        }

        val annotations = deserializeAnnotations(proto.annotations)
        declaration.annotations.addAll(annotations)

        parent?.let { declaration.parent = it }

        val descriptor = declaration.descriptor

        if (descriptor is WrappedDeclarationDescriptor<*>) {
            when (declaration) {
                is IrValueParameter ->
                    if (descriptor is WrappedValueParameterDescriptor) descriptor.bind(declaration)
                    else (descriptor as WrappedReceiverParameterDescriptor).bind(declaration)
                is IrVariable -> (descriptor as WrappedVariableDescriptor).bind(declaration)
                is IrTypeParameter -> (descriptor as WrappedTypeParameterDescriptor).bind(declaration)
                is IrAnonymousInitializer -> (descriptor as WrappedClassDescriptor).bind(parent!! as IrClass)
                is IrClass -> (descriptor as WrappedClassDescriptor).bind(declaration)
                is IrConstructor -> (descriptor as WrappedClassConstructorDescriptor).bind(declaration)
                is IrField -> (descriptor as WrappedFieldDescriptor).bind(declaration)
                is IrProperty -> (descriptor as WrappedPropertyDescriptor).bind(declaration)
                is IrEnumEntry -> (descriptor as WrappedEnumEntryDescriptor).bind(declaration)
                is IrSimpleFunction -> (descriptor as WrappedSimpleFunctionDescriptor).bind(declaration)
            }
        }
        logger.log { "### Deserialized declaration: ${descriptor} -> ${ir2string(declaration)}" }

        return declaration
    }
}
