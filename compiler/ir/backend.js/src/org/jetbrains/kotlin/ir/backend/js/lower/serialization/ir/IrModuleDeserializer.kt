/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

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
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.IrKlibProtoBuf.IrStatement.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.IrKlibProtoBuf.IrOperation.OperationCase.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.IrKlibProtoBuf.IrConst.ValueCase.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.IrKlibProtoBuf.IrDeclarator.DeclaratorCase.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.IrKlibProtoBuf.IrType.KindCase.*


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

    abstract fun deserializeIrSymbol(proto: IrKlibProtoBuf.IrSymbol): IrSymbol
    abstract fun deserializeIrType(proto: IrKlibProtoBuf.IrTypeIndex): IrType
    abstract fun deserializeDescriptorReference(proto: IrKlibProtoBuf.DescriptorReference): DeclarationDescriptor
    abstract fun deserializeString(proto: IrKlibProtoBuf.String): String

    private fun deserializeTypeArguments(proto: IrKlibProtoBuf.TypeArguments): List<IrType> {
        logger.log { "### deserializeTypeArguments" }
        val result = mutableListOf<IrType>()
        proto.typeArgumentList.forEach { typeProto ->
            val type = deserializeIrType(typeProto)
            result.add(type)
            logger.log { "$type" }
        }
        return result
    }

    fun deserializeIrTypeVariance(variance: IrKlibProtoBuf.IrTypeVariance) = when (variance) {
        IrKlibProtoBuf.IrTypeVariance.IN -> Variance.IN_VARIANCE
        IrKlibProtoBuf.IrTypeVariance.OUT -> Variance.OUT_VARIANCE
        IrKlibProtoBuf.IrTypeVariance.INV -> Variance.INVARIANT
    }

    fun deserializeIrTypeArgument(proto: IrKlibProtoBuf.IrTypeArgument) = when (proto.kindCase) {
        IrKlibProtoBuf.IrTypeArgument.KindCase.STAR -> IrStarProjectionImpl
        IrKlibProtoBuf.IrTypeArgument.KindCase.TYPE -> makeTypeProjection(
            deserializeIrType(proto.type.type), deserializeIrTypeVariance(proto.type.variance)
        )
        else -> TODO("Unexpected projection kind")

    }

    fun deserializeAnnotations(annotations: IrKlibProtoBuf.Annotations): List<IrCall> {
        return annotations.annotationList.map {
            deserializeCall(it, 0, 0, builtIns.unitType) // TODO: need a proper deserialization here
        }
    }

    fun deserializeSimpleType(proto: IrKlibProtoBuf.IrSimpleType): IrSimpleType {
        val symbol = deserializeIrSymbol(proto.classifier) as? IrClassifierSymbol
            //?: error("could not convert sym to ClassifierSym ${proto.classifier.kind} ${proto.classifier.uniqId.index} ${proto.classifier.uniqId.isLocal}")
            ?: error("could not convert sym to ClassifierSymbol")
        logger.log { "deserializeSimpleType: symbol=$symbol" }

        val result = builtIns.getPrimitiveTypeOrNullByDescriptor(symbol.descriptor, proto.hasQuestionMark) ?: run {
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

    fun deserializeDynamicType(proto: IrKlibProtoBuf.IrDynamicType): IrDynamicType {
        val annotations = deserializeAnnotations(proto.annotations)
        return IrDynamicTypeImpl(null, annotations, Variance.INVARIANT)
    }

    fun deserializeErrorType(proto: IrKlibProtoBuf.IrErrorType): IrErrorType {
        val annotations = deserializeAnnotations(proto.annotations)
        return IrErrorTypeImpl(null, annotations, Variance.INVARIANT)
    }

    fun deserializeIrTypeData(proto: IrKlibProtoBuf.IrType): IrType {
        return when (proto.kindCase) {
            SIMPLE -> deserializeSimpleType(proto.simple)
            DYNAMIC -> deserializeDynamicType(proto.dynamic)
            ERROR -> deserializeErrorType(proto.error)
            else -> TODO("Unexpected IrType kind: ${proto.kindCase}")
        }
    }

    /* -------------------------------------------------------------- */

    private fun deserializeBlockBody(
        proto: IrKlibProtoBuf.IrBlockBody,
        start: Int, end: Int
    ): IrBlockBody {

        val statements = mutableListOf<IrStatement>()

        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        return IrBlockBodyImpl(start, end, statements)
    }

    private fun deserializeBranch(proto: IrKlibProtoBuf.IrBranch, start: Int, end: Int): IrBranch {

        val condition = deserializeExpression(proto.condition)
        val result = deserializeExpression(proto.result)

        return IrBranchImpl(start, end, condition, result)
    }

    private fun deserializeCatch(proto: IrKlibProtoBuf.IrCatch, start: Int, end: Int): IrCatch {
        val catchParameter =
            deserializeDeclaration(proto.catchParameter, null) as IrVariable // TODO: we need a proper parent here
        val result = deserializeExpression(proto.result)

        val catch = IrCatchImpl(start, end, catchParameter, result)
        return catch
    }

    private fun deserializeSyntheticBody(proto: IrKlibProtoBuf.IrSyntheticBody, start: Int, end: Int): IrSyntheticBody {
        val kind = when (proto.kind) {
            IrKlibProtoBuf.IrSyntheticBodyKind.ENUM_VALUES -> IrSyntheticBodyKind.ENUM_VALUES
            IrKlibProtoBuf.IrSyntheticBodyKind.ENUM_VALUEOF -> IrSyntheticBodyKind.ENUM_VALUEOF
        }
        return IrSyntheticBodyImpl(start, end, kind)
    }

    private fun deserializeStatement(proto: IrKlibProtoBuf.IrStatement): IrElement {
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

    private fun deserializeBlock(proto: IrKlibProtoBuf.IrBlock, start: Int, end: Int, type: IrType): IrBlock {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        val isLambdaOrigin = if (proto.isLambdaOrigin) IrStatementOrigin.LAMBDA else null

        return IrBlockImpl(start, end, type, isLambdaOrigin, statements)
    }

    private fun deserializeMemberAccessCommon(access: IrMemberAccessExpression, proto: IrKlibProtoBuf.MemberAccessCommon) {

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
        proto: IrKlibProtoBuf.IrClassReference,
        start: Int,
        end: Int,
        type: IrType
    ): IrClassReference {
        val symbol = deserializeIrSymbol(proto.classSymbol) as IrClassifierSymbol
        val classType = deserializeIrType(proto.classType)
        /** TODO: [createClassifierSymbolForClassReference] is internal function */
        return IrClassReferenceImpl(start, end, type, symbol, classType)
    }

    private fun deserializeCall(proto: IrKlibProtoBuf.IrCall, start: Int, end: Int, type: IrType): IrCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrFunctionSymbol

        val superSymbol = if (proto.hasSuper()) {
            deserializeIrSymbol(proto.`super`) as IrClassSymbol
        } else null

        val call: IrCall = when (proto.kind) {
            IrKlibProtoBuf.IrCall.Primitive.NOT_PRIMITIVE ->
                // TODO: implement the last three args here.
                IrCallImpl(
                    start, end, type,
                    symbol, symbol.descriptor,
                    proto.memberAccess.typeArguments.typeArgumentCount,
                    proto.memberAccess.valueArgumentList.size,
                    null, superSymbol
                )
            IrKlibProtoBuf.IrCall.Primitive.NULLARY ->
                IrNullaryPrimitiveImpl(start, end, type, null, symbol)
            IrKlibProtoBuf.IrCall.Primitive.UNARY ->
                IrUnaryPrimitiveImpl(start, end, type, null, symbol)
            IrKlibProtoBuf.IrCall.Primitive.BINARY ->
                IrBinaryPrimitiveImpl(start, end, type, null, symbol)
            else -> TODO("Unexpected primitive IrCall.")
        }
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    private fun deserializeComposite(proto: IrKlibProtoBuf.IrComposite, start: Int, end: Int, type: IrType): IrComposite {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }
        return IrCompositeImpl(start, end, type, null, statements)
    }

    private fun deserializeDelegatingConstructorCall(
        proto: IrKlibProtoBuf.IrDelegatingConstructorCall,
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
        proto: IrKlibProtoBuf.IrEnumConstructorCall,
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
        proto: IrKlibProtoBuf.IrFunctionReference,
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

    private fun deserializeGetClass(proto: IrKlibProtoBuf.IrGetClass, start: Int, end: Int, type: IrType): IrGetClass {
        val argument = deserializeExpression(proto.argument)
        return IrGetClassImpl(start, end, type, argument)
    }

    private fun deserializeGetField(proto: IrKlibProtoBuf.IrGetField, start: Int, end: Int, type: IrType): IrGetField {
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

    private fun deserializeGetValue(proto: IrKlibProtoBuf.IrGetValue, start: Int, end: Int, type: IrType): IrGetValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrValueSymbol
        // TODO: origin!
        return IrGetValueImpl(start, end, type, symbol, null)
    }

    private fun deserializeGetEnumValue(proto: IrKlibProtoBuf.IrGetEnumValue, start: Int, end: Int, type: IrType): IrGetEnumValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrEnumEntrySymbol
        return IrGetEnumValueImpl(start, end, type, symbol)
    }

    private fun deserializeGetObject(proto: IrKlibProtoBuf.IrGetObject, start: Int, end: Int, type: IrType): IrGetObjectValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrGetObjectValueImpl(start, end, type, symbol)
    }

    private fun deserializeInstanceInitializerCall(
        proto: IrKlibProtoBuf.IrInstanceInitializerCall,
        start: Int,
        end: Int
    ): IrInstanceInitializerCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrInstanceInitializerCallImpl(start, end, symbol, builtIns.unitType)
    }

    private val getterToPropertyDescriptorMap = mutableMapOf<IrSimpleFunctionSymbol, WrappedPropertyDescriptor>()

    private fun deserializePropertyReference(
        proto: IrKlibProtoBuf.IrPropertyReference,
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

    private fun deserializeReturn(proto: IrKlibProtoBuf.IrReturn, start: Int, end: Int, type: IrType): IrReturn {
        val symbol = deserializeIrSymbol(proto.returnTarget) as IrReturnTargetSymbol
        val value = deserializeExpression(proto.value)
        return IrReturnImpl(start, end, builtIns.nothingType, symbol, value)
    }

    private fun deserializeSetField(proto: IrKlibProtoBuf.IrSetField, start: Int, end: Int): IrSetField {
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

    private fun deserializeSetVariable(proto: IrKlibProtoBuf.IrSetVariable, start: Int, end: Int): IrSetVariable {
        val symbol = deserializeIrSymbol(proto.symbol) as IrVariableSymbol
        val value = deserializeExpression(proto.value)
        return IrSetVariableImpl(start, end, builtIns.unitType, symbol, value, null)
    }

    private fun deserializeSpreadElement(proto: IrKlibProtoBuf.IrSpreadElement): IrSpreadElement {
        val expression = deserializeExpression(proto.expression)
        return IrSpreadElementImpl(proto.coordinates.startOffset, proto.coordinates.endOffset, expression)
    }

    private fun deserializeStringConcat(
        proto: IrKlibProtoBuf.IrStringConcat,
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

    private fun deserializeThrow(proto: IrKlibProtoBuf.IrThrow, start: Int, end: Int, type: IrType): IrThrowImpl {
        return IrThrowImpl(start, end, builtIns.nothingType, deserializeExpression(proto.value))
    }

    private fun deserializeTry(proto: IrKlibProtoBuf.IrTry, start: Int, end: Int, type: IrType): IrTryImpl {
        val result = deserializeExpression(proto.result)
        val catches = mutableListOf<IrCatch>()
        proto.catchList.forEach {
            catches.add(deserializeStatement(it) as IrCatch)
        }
        val finallyExpression =
            if (proto.hasFinally()) deserializeExpression(proto.getFinally()) else null
        return IrTryImpl(start, end, type, result, catches, finallyExpression)
    }

    private fun deserializeTypeOperator(operator: IrKlibProtoBuf.IrTypeOperator) = when (operator) {
        IrKlibProtoBuf.IrTypeOperator.CAST
        -> IrTypeOperator.CAST
        IrKlibProtoBuf.IrTypeOperator.IMPLICIT_CAST
        -> IrTypeOperator.IMPLICIT_CAST
        IrKlibProtoBuf.IrTypeOperator.IMPLICIT_NOTNULL
        -> IrTypeOperator.IMPLICIT_NOTNULL
        IrKlibProtoBuf.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        -> IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        IrKlibProtoBuf.IrTypeOperator.IMPLICIT_INTEGER_COERCION
        -> IrTypeOperator.IMPLICIT_INTEGER_COERCION
        IrKlibProtoBuf.IrTypeOperator.SAFE_CAST
        -> IrTypeOperator.SAFE_CAST
        IrKlibProtoBuf.IrTypeOperator.INSTANCEOF
        -> IrTypeOperator.INSTANCEOF
        IrKlibProtoBuf.IrTypeOperator.NOT_INSTANCEOF
        -> IrTypeOperator.NOT_INSTANCEOF
        IrKlibProtoBuf.IrTypeOperator.SAM_CONVERSION
        -> IrTypeOperator.SAM_CONVERSION
    }

    private fun deserializeTypeOp(proto: IrKlibProtoBuf.IrTypeOp, start: Int, end: Int, type: IrType): IrTypeOperatorCall {
        val operator = deserializeTypeOperator(proto.operator)
        val operand = deserializeIrType(proto.operand)//.brokenIr
        val argument = deserializeExpression(proto.argument)
        return IrTypeOperatorCallImpl(start, end, type, operator, operand).apply {
            this.argument = argument
            this.typeOperandClassifier = operand.classifierOrFail
        }
    }

    private fun deserializeVararg(proto: IrKlibProtoBuf.IrVararg, start: Int, end: Int, type: IrType): IrVararg {
        val elementType = deserializeIrType(proto.elementType)

        val elements = mutableListOf<IrVarargElement>()
        proto.elementList.forEach {
            elements.add(deserializeVarargElement(it))
        }
        return IrVarargImpl(start, end, type, elementType, elements)
    }

    private fun deserializeVarargElement(element: IrKlibProtoBuf.IrVarargElement): IrVarargElement {
        return when (element.varargElementCase) {
            IrKlibProtoBuf.IrVarargElement.VarargElementCase.EXPRESSION
            -> deserializeExpression(element.expression)
            IrKlibProtoBuf.IrVarargElement.VarargElementCase.SPREAD_ELEMENT
            -> deserializeSpreadElement(element.spreadElement)
            else
            -> TODO("Unexpected vararg element")
        }
    }

    private fun deserializeWhen(proto: IrKlibProtoBuf.IrWhen, start: Int, end: Int, type: IrType): IrWhen {
        val branches = mutableListOf<IrBranch>()

        proto.branchList.forEach {
            branches.add(deserializeStatement(it) as IrBranch)
        }

        // TODO: provide some origin!
        return IrWhenImpl(start, end, type, null, branches)
    }

    private val loopIndex = mutableMapOf<Int, IrLoop>()

    private fun deserializeLoop(proto: IrKlibProtoBuf.Loop, loop: IrLoopBase): IrLoopBase {
        val loopId = proto.loopId
        loopIndex.getOrPut(loopId) { loop }

        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val body = if (proto.hasBody()) deserializeExpression(proto.body) else null
        val condition = deserializeExpression(proto.condition)

        loop.label = label
        loop.condition = condition
        loop.body = body

        return loop
    }

    private fun deserializeDoWhile(proto: IrKlibProtoBuf.IrDoWhile, start: Int, end: Int, type: IrType): IrDoWhileLoop {
        // we create the loop before deserializing the body, so that
        // IrBreak statements have something to put into 'loop' field.
        val loop = IrDoWhileLoopImpl(start, end, type, null)
        deserializeLoop(proto.loop, loop)
        return loop
    }

    private fun deserializeWhile(proto: IrKlibProtoBuf.IrWhile, start: Int, end: Int, type: IrType): IrWhileLoop {
        // we create the loop before deserializing the body, so that
        // IrBreak statements have something to put into 'loop' field.
        val loop = IrWhileLoopImpl(start, end, type, null)
        deserializeLoop(proto.loop, loop)
        return loop
    }

    private fun deserializeDynamicMemberExpression(proto: IrKlibProtoBuf.IrDynamicMemberExpression, start: Int, end: Int, type: IrType) =
        IrDynamicMemberExpressionImpl(start, end, type, deserializeString(proto.memberName), deserializeExpression(proto.receiver))

    private fun deserializeDynamicOperatorExpression(proto: IrKlibProtoBuf.IrDynamicOperatorExpression, start: Int, end: Int, type: IrType) =
        IrDynamicOperatorExpressionImpl(start, end, type, deserializeDynamicOperator(proto.operator)).apply {
            receiver = deserializeExpression(proto.receiver)
            proto.argumentList.mapTo(arguments) { deserializeExpression(it) }
        }

    private fun deserializeDynamicOperator(operator: IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator) = when (operator) {
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.UNARY_PLUS -> IrDynamicOperator.UNARY_PLUS
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.UNARY_MINUS -> IrDynamicOperator.UNARY_MINUS

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.EXCL -> IrDynamicOperator.EXCL

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.PREFIX_INCREMENT -> IrDynamicOperator.PREFIX_INCREMENT
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.PREFIX_DECREMENT -> IrDynamicOperator.PREFIX_DECREMENT

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.POSTFIX_INCREMENT -> IrDynamicOperator.POSTFIX_INCREMENT
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.POSTFIX_DECREMENT -> IrDynamicOperator.POSTFIX_DECREMENT

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.BINARY_PLUS -> IrDynamicOperator.BINARY_PLUS
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.BINARY_MINUS -> IrDynamicOperator.BINARY_MINUS
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.MUL -> IrDynamicOperator.MUL
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.DIV -> IrDynamicOperator.DIV
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.MOD -> IrDynamicOperator.MOD

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.GT -> IrDynamicOperator.GT
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.LT -> IrDynamicOperator.LT
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.GE -> IrDynamicOperator.GE
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.LE -> IrDynamicOperator.LE

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.EQEQ -> IrDynamicOperator.EQEQ
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.EXCLEQ -> IrDynamicOperator.EXCLEQ

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.EQEQEQ -> IrDynamicOperator.EQEQEQ
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.EXCLEQEQ -> IrDynamicOperator.EXCLEQEQ

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.ANDAND -> IrDynamicOperator.ANDAND
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.OROR -> IrDynamicOperator.OROR

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.EQ -> IrDynamicOperator.EQ
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.PLUSEQ -> IrDynamicOperator.PLUSEQ
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.MINUSEQ -> IrDynamicOperator.MINUSEQ
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.MULEQ -> IrDynamicOperator.MULEQ
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.DIVEQ -> IrDynamicOperator.DIVEQ
        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.MODEQ -> IrDynamicOperator.MODEQ

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.ARRAY_ACCESS -> IrDynamicOperator.ARRAY_ACCESS

        IrKlibProtoBuf.IrDynamicOperatorExpression.IrDynamicOperator.INVOKE -> IrDynamicOperator.INVOKE
    }

    private fun deserializeBreak(proto: IrKlibProtoBuf.IrBreak, start: Int, end: Int, type: IrType): IrBreak {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val loopId = proto.loopId
        val loop = loopIndex[loopId]!!
        val irBreak = IrBreakImpl(start, end, type, loop)
        irBreak.label = label

        return irBreak
    }

    private fun deserializeContinue(proto: IrKlibProtoBuf.IrContinue, start: Int, end: Int, type: IrType): IrContinue {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val loopId = proto.loopId
        val loop = loopIndex[loopId]!!
        val irContinue = IrContinueImpl(start, end, type, loop)
        irContinue.label = label

        return irContinue
    }

    private fun deserializeConst(proto: IrKlibProtoBuf.IrConst, start: Int, end: Int, type: IrType): IrExpression =
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

    private fun deserializeOperation(proto: IrKlibProtoBuf.IrOperation, start: Int, end: Int, type: IrType): IrExpression =
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

    private fun deserializeExpression(proto: IrKlibProtoBuf.IrExpression): IrExpression {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val type = deserializeIrType(proto.type)
        val operation = proto.operation
        val expression = deserializeOperation(operation, start, end, type)

        logger.log { "### Deserialized expression: ${ir2string(expression)} ir_type=$type" }
        return expression
    }

    private fun deserializeIrTypeParameter(
        proto: IrKlibProtoBuf.IrTypeParameter,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrTypeParameter {
        val symbol = deserializeIrSymbol(proto.symbol) as IrTypeParameterSymbol
        val name = Name.identifier(deserializeString(proto.name))
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
        proto: IrKlibProtoBuf.IrTypeParameterContainer,
        parent: IrDeclarationParent
    ): List<IrTypeParameter> {
        return proto.typeParameterList.map {
            deserializeDeclaration(
                it,
                parent
            ) as IrTypeParameter
        } // TODO: we need proper start, end and origin here?
    }

    private fun deserializeClassKind(kind: IrKlibProtoBuf.ClassKind) = when (kind) {
        IrKlibProtoBuf.ClassKind.CLASS -> ClassKind.CLASS
        IrKlibProtoBuf.ClassKind.INTERFACE -> ClassKind.INTERFACE
        IrKlibProtoBuf.ClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
        IrKlibProtoBuf.ClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
        IrKlibProtoBuf.ClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        IrKlibProtoBuf.ClassKind.OBJECT -> ClassKind.OBJECT
    }

    private fun deserializeIrValueParameter(
        proto: IrKlibProtoBuf.IrValueParameter,
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
                deserializeString(proto.name).let { if (paramSymbol.descriptor is ReceiverParameterDescriptor) Name.special(it) else Name.identifier(it) },
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

    private fun deserializeIrClass(proto: IrKlibProtoBuf.IrClass, start: Int, end: Int, origin: IrDeclarationOrigin): IrClass {

        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol

        val modality = deserializeModality(proto.modality)
        val clazz = symbolTable.declareClass(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
            symbol.descriptor, modality) {
            IrClassImpl(
                        start, end, origin,
                        it,
                        deserializeString(proto.name).let { if (it.startsWith('<')) Name.special(it) else Name.identifier(it) },
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
        base: IrKlibProtoBuf.IrFunctionBase,
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
        proto: IrKlibProtoBuf.IrFunction,
        start: Int, end: Int, origin: IrDeclarationOrigin, isAccessor: Boolean
    ): IrSimpleFunction {

        logger.log { "### deserializing IrFunction ${proto.base.name}" }
        val symbol = deserializeIrSymbol(proto.symbol) as IrSimpleFunctionSymbol

        val function = symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
            symbol.descriptor, {
                IrFunctionImpl(
                    start, end, origin, it,
                    deserializeString(proto.base.name).let { if (isAccessor) Name.special(it) else Name.identifier(it) },
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
        proto: IrKlibProtoBuf.IrVariable,
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
            Name.identifier(deserializeString(proto.name)),
            type,
            proto.isVar,
            proto.isConst,
            proto.isLateinit
        )
        variable.initializer = initializer
        return variable
    }

    private fun deserializeIrEnumEntry(
        proto: IrKlibProtoBuf.IrEnumEntry,
        start: Int, end: Int, origin: IrDeclarationOrigin
    ): IrEnumEntry {
        val symbol = deserializeIrSymbol(proto.symbol) as IrEnumEntrySymbol

        val enumEntry = symbolTable.declareEnumEntry(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            irrelevantOrigin,
            symbol.descriptor
        ) {
            IrEnumEntryImpl(start, end, origin, it, Name.identifier(deserializeString(proto.name)))
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
        proto: IrKlibProtoBuf.IrAnonymousInit,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrAnonymousInitializer {
        val symbol = deserializeIrSymbol(proto.symbol) as IrAnonymousInitializerSymbol
        val initializer = IrAnonymousInitializerImpl(start, end, origin, symbol)
        initializer.body = deserializeBlockBody(proto.body.blockBody, start, end)
        return initializer
    }

    private fun deserializeVisibility(value: IrKlibProtoBuf.Visibility): Visibility { // TODO: switch to enum
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
        proto: IrKlibProtoBuf.IrConstructor,
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
                    Name.special(deserializeString(proto.base.name)),
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

    private fun deserializeIrField(proto: IrKlibProtoBuf.IrField, start: Int, end: Int, origin: IrDeclarationOrigin): IrField {

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
                    Name.identifier(deserializeString(proto.name)),
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

    private fun deserializeModality(modality: IrKlibProtoBuf.ModalityKind) = when (modality) {
        IrKlibProtoBuf.ModalityKind.OPEN_MODALITY -> Modality.OPEN
        IrKlibProtoBuf.ModalityKind.SEALED_MODALITY -> Modality.SEALED
        IrKlibProtoBuf.ModalityKind.FINAL_MODALITY -> Modality.FINAL
        IrKlibProtoBuf.ModalityKind.ABSTRACT_MODALITY -> Modality.ABSTRACT
    }

    private fun deserializeIrProperty(
        proto: IrKlibProtoBuf.IrProperty,
        start: Int,
        end: Int,
        origin: IrDeclarationOrigin
    ): IrProperty {

        val backingField = if (proto.hasBackingField()) deserializeIrField(proto.backingField, start, end, origin) else null
        val getter = if (proto.hasGetter()) deserializeIrFunction(proto.getter, start, end, origin, true) else null
        val setter = if (proto.hasSetter()) deserializeIrFunction(proto.setter, start, end, origin, true) else null

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
            Name.identifier(deserializeString(proto.name)),
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
        proto: IrKlibProtoBuf.IrTypeAlias,
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
    fun deserializeIrDeclarationOrigin(proto: IrKlibProtoBuf.IrDeclarationOrigin) = originIndex[deserializeString(proto.custom)]!!

    protected fun deserializeDeclaration(proto: IrKlibProtoBuf.IrDeclaration, parent: IrDeclarationParent?): IrDeclaration {

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
            -> deserializeIrFunction(declarator.irFunction, start, end, origin, false)
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
