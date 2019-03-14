/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.library.CombinedIrFileWriter
import org.jetbrains.kotlin.backend.common.library.DeclarationId
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrNullaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrUnaryPrimitiveImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

open class IrModuleSerializer(
    val logger: LoggingContext,
    val declarationTable: DeclarationTable,
    val mangler: KotlinMangler,
    val bodiesOnlyForInlines: Boolean = false
) {

    private val loopIndex = mutableMapOf<IrLoop, Int>()
    private var currentLoopIndex = 0
    val descriptorReferenceSerializer = DescriptorReferenceSerializer(declarationTable, { string -> serializeString(string) }, mangler)

    // The same symbol can be used multiple times in a module
    // so use this index to store symbol data only once.
    val protoSymbolMap = mutableMapOf<IrSymbol, Int>()
    val protoSymbolArray = arrayListOf<KotlinIr.IrSymbolData>()

    // The same type can be used multiple times in a module
    // so use this index to store type data only once.
    val protoTypeMap = mutableMapOf<IrTypeKey, Int>()
    val protoTypeArray = arrayListOf<KotlinIr.IrType>()

    val protoStringMap = mutableMapOf<String, Int>()
    val protoStringArray = arrayListOf<String>()

    /* ------- Common fields ---------------------------------------------------- */

    private fun serializeIrDeclarationOrigin(origin: IrDeclarationOrigin) =
        KotlinIr.IrDeclarationOrigin.newBuilder()
            .setCustom(serializeString((origin as IrDeclarationOriginImpl).name))
            .build()

    private fun serializeIrStatementOrigin(origin: IrStatementOrigin) =
        KotlinIr.IrStatementOrigin.newBuilder()
            .setName(serializeString((origin as IrStatementOriginImpl).debugName))
            .build()

    private fun serializeVisibility(visibility: Visibility) =
        KotlinIr.Visibility.newBuilder()
            .setName(serializeString(visibility.name))
            .build()

    private fun serializeCoordinates(start: Int, end: Int): KotlinIr.Coordinates {
        return KotlinIr.Coordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build()
    }

    /* ------- Strings ---------------------------------------------------------- */

    fun serializeString(value: String): KotlinIr.String {
        val proto = KotlinIr.String.newBuilder()
        proto.index = protoStringMap.getOrPut(value) {
            protoStringArray.add(value)
            protoStringArray.size - 1
        }
        return proto.build()
    }

    fun serializeName(name: Name): KotlinIr.String = serializeString(name.toString())

    /* ------- IrSymbols -------------------------------------------------------- */

    fun protoSymbolKind(symbol: IrSymbol): KotlinIr.IrSymbolKind = when (symbol) {
        is IrAnonymousInitializerSymbol ->
            KotlinIr.IrSymbolKind.ANONYMOUS_INIT_SYMBOL
        is IrClassSymbol ->
            KotlinIr.IrSymbolKind.CLASS_SYMBOL
        is IrConstructorSymbol ->
            KotlinIr.IrSymbolKind.CONSTRUCTOR_SYMBOL
        is IrTypeParameterSymbol ->
            KotlinIr.IrSymbolKind.TYPE_PARAMETER_SYMBOL
        is IrEnumEntrySymbol ->
            KotlinIr.IrSymbolKind.ENUM_ENTRY_SYMBOL
        is IrVariableSymbol ->
            KotlinIr.IrSymbolKind.VARIABLE_SYMBOL
        is IrValueParameterSymbol ->
            if (symbol.descriptor is ReceiverParameterDescriptor) // TODO: we use descriptor here.
                KotlinIr.IrSymbolKind.RECEIVER_PARAMETER_SYMBOL
            else
                KotlinIr.IrSymbolKind.VALUE_PARAMETER_SYMBOL
        is IrSimpleFunctionSymbol ->
            KotlinIr.IrSymbolKind.FUNCTION_SYMBOL
        is IrReturnableBlockSymbol ->
            KotlinIr.IrSymbolKind.RETURNABLE_BLOCK_SYMBOL
        is IrFieldSymbol ->
            if (symbol.owner.correspondingProperty.let { it == null || it.isDelegated })
                KotlinIr.IrSymbolKind.STANDALONE_FIELD_SYMBOL
            else
                KotlinIr.IrSymbolKind.FIELD_SYMBOL
        else ->
            TODO("Unexpected symbol kind: $symbol")
    }

    fun serializeIrSymbolData(symbol: IrSymbol): KotlinIr.IrSymbolData {

        val declaration = symbol.owner as? IrDeclaration ?: error("Expected IrDeclaration: ${symbol.owner}")

        val proto = KotlinIr.IrSymbolData.newBuilder()
        proto.kind = protoSymbolKind(symbol)

        val uniqId =
            declarationTable.uniqIdByDeclaration(declaration)
        proto.setUniqId(protoUniqId(uniqId))

        val topLevelUniqId =
            declarationTable.uniqIdByDeclaration((declaration).findTopLevelDeclaration())
        proto.setTopLevelUniqId(protoUniqId(topLevelUniqId))

        descriptorReferenceSerializer.serializeDescriptorReference(declaration) ?. let {
            proto.setDescriptorReference(it)
        }

        return proto.build()
    }

    fun serializeIrSymbol(symbol: IrSymbol): KotlinIr.IrSymbol {
        val proto = KotlinIr.IrSymbol.newBuilder()
        proto.index = protoSymbolMap.getOrPut(symbol) {
            protoSymbolArray.add(serializeIrSymbolData(symbol))
            protoSymbolArray.size - 1
        }
        return proto.build()
    }

    /* ------- IrTypes ---------------------------------------------------------- */

    // TODO: we, probably, need a type table.

    private fun serializeTypeArguments(call: IrMemberAccessExpression): KotlinIr.TypeArguments {
        val proto = KotlinIr.TypeArguments.newBuilder()
        for (i in 0 until call.typeArgumentsCount) {
            proto.addTypeArgument(serializeIrType(call.getTypeArgument(i)!!))
        }
        return proto.build()
    }

    fun serializeIrTypeVariance(variance: Variance) = when (variance) {
        Variance.IN_VARIANCE -> KotlinIr.IrTypeVariance.IN
        Variance.OUT_VARIANCE -> KotlinIr.IrTypeVariance.OUT
        Variance.INVARIANT -> KotlinIr.IrTypeVariance.INV
    }

    fun serializeAnnotations(annotations: List<IrCall>): KotlinIr.Annotations {
        val proto = KotlinIr.Annotations.newBuilder()
        annotations.forEach {
            proto.addAnnotation(serializeCall(it))
        }
        return proto.build()
    }

    fun serializeIrTypeProjection(argument: IrTypeProjection) = KotlinIr.IrTypeProjection.newBuilder()
        .setVariance(serializeIrTypeVariance(argument.variance))
        .setType(serializeIrType(argument.type))
        .build()

    fun serializeTypeArgument(argument: IrTypeArgument): KotlinIr.IrTypeArgument {
        val proto = KotlinIr.IrTypeArgument.newBuilder()
        when (argument) {
            is IrStarProjection ->
                proto.star = KotlinIr.IrStarProjection.newBuilder()
                    .build() // TODO: Do we need a singletone here? Or just an enum?
            is IrTypeProjection ->
                proto.type = serializeIrTypeProjection(argument)
            else -> TODO("Unexpected type argument kind: $argument")
        }
        return proto.build()
    }

    fun serializeSimpleType(type: IrSimpleType): KotlinIr.IrSimpleType {
        val proto = KotlinIr.IrSimpleType.newBuilder()
            .setAnnotations(serializeAnnotations(type.annotations))
            .setClassifier(serializeIrSymbol(type.classifier))
            .setHasQuestionMark(type.hasQuestionMark)
        type.arguments.forEach {
            proto.addArgument(serializeTypeArgument(it))
        }
        return proto.build()
    }

    fun serializeDynamicType(type: IrDynamicType) = KotlinIr.IrDynamicType.newBuilder()
        .setAnnotations(serializeAnnotations(type.annotations))
        .build()

    fun serializeErrorType(type: IrErrorType) = KotlinIr.IrErrorType.newBuilder()
        .setAnnotations(serializeAnnotations(type.annotations))
        .build()

    private fun serializeIrTypeData(type: IrType): KotlinIr.IrType {
        logger.log { "### serializing IrType: " + type }
        val proto = KotlinIr.IrType.newBuilder()
        when (type) {
            is IrSimpleType ->
                proto.simple = serializeSimpleType(type)
            is IrDynamicType ->
                proto.dynamic = serializeDynamicType(type)
            is IrErrorType ->
                proto.error = serializeErrorType(type)
            else -> TODO("IrType serialization not implemented yet: $type.")
        }
        return proto.build()
    }

    enum class IrTypeKind {
        SIMPLE,
        DYNAMIC,
        ERROR
    }

    enum class IrTypeArgumentKind {
        STAR,
        PROJECTION
    }

    // This is just IrType repacked as a data class, good to address a hash map.
    data class IrTypeKey (
        val kind: IrTypeKind,
        val classifier: IrClassifierSymbol?,
        val hasQuestionMark: Boolean?,
        val arguments: List<IrTypeArgumentKey>?,
        val annotations: List<IrCall>
    )

    data class IrTypeArgumentKey (
        val kind: IrTypeArgumentKind,
        val variance: Variance?,
        val type: IrTypeKey?
    )

    val IrType.toIrTypeKey: IrTypeKey get() = IrTypeKey(
        kind = when (this) {
            is IrSimpleType -> IrTypeKind.SIMPLE
            is IrDynamicType -> IrTypeKind.DYNAMIC
            is IrErrorType -> IrTypeKind.ERROR
            else -> error("Unexpected IrType kind: $this")
        },
        classifier = this.classifierOrNull,
        hasQuestionMark = (this as? IrSimpleType)?.hasQuestionMark,
        arguments = (this as? IrSimpleType)?.arguments?.map { it.toIrTypeArgumentKey },
        annotations = this.annotations
    )

    val IrTypeArgument.toIrTypeArgumentKey: IrTypeArgumentKey get() = IrTypeArgumentKey(
        kind = when (this) {
            is IrStarProjection -> IrTypeArgumentKind.STAR
            is IrTypeProjection -> IrTypeArgumentKind.PROJECTION
            else -> error("Unexpected type argument kind: $this")
        },
        variance = (this as? IrTypeProjection)?.variance,
        type = (this as? IrTypeProjection)?.type?.toIrTypeKey
    )

    fun serializeIrType(type: IrType): KotlinIr.IrTypeIndex {
        val proto = KotlinIr.IrTypeIndex.newBuilder()
        val key = type.toIrTypeKey
        proto.index = protoTypeMap.getOrPut(key) {
            // println("new type: $type ${(type as? IrSimpleType)?.classifier?.descriptor}${if((type as? IrSimpleType)?.hasQuestionMark ?: false) "?" else ""}")
            // println("new key = $key")
            protoTypeArray.add(serializeIrTypeData(type))
            protoTypeArray.size - 1
        }
        return proto.build()
    }

    /* -------------------------------------------------------------------------- */

    private fun serializeBlockBody(expression: IrBlockBody): KotlinIr.IrBlockBody {
        val proto = KotlinIr.IrBlockBody.newBuilder()
        expression.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeBranch(branch: IrBranch): KotlinIr.IrBranch {
        val proto = KotlinIr.IrBranch.newBuilder()

        proto.condition = serializeExpression(branch.condition)
        proto.result = serializeExpression(branch.result)

        return proto.build()
    }

    private fun serializeBlock(block: IrBlock): KotlinIr.IrBlock {
        val isLambdaOrigin =
            block.origin == IrStatementOrigin.LAMBDA ||
                    block.origin == IrStatementOrigin.ANONYMOUS_FUNCTION
        val proto = KotlinIr.IrBlock.newBuilder()
            .setIsLambdaOrigin(isLambdaOrigin)
        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeComposite(composite: IrComposite): KotlinIr.IrComposite {
        val proto = KotlinIr.IrComposite.newBuilder()
        composite.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeCatch(catch: IrCatch): KotlinIr.IrCatch {
        val proto = KotlinIr.IrCatch.newBuilder()
            .setCatchParameter(serializeDeclaration(catch.catchParameter))
            .setResult(serializeExpression(catch.result))
        return proto.build()
    }

    private fun serializeStringConcat(expression: IrStringConcatenation): KotlinIr.IrStringConcat {
        val proto = KotlinIr.IrStringConcat.newBuilder()
        expression.arguments.forEach {
            proto.addArgument(serializeExpression(it))
        }
        return proto.build()
    }

    private fun irCallToPrimitiveKind(call: IrCall): KotlinIr.IrCall.Primitive = when (call) {
        is IrNullaryPrimitiveImpl
        -> KotlinIr.IrCall.Primitive.NULLARY
        is IrUnaryPrimitiveImpl
        -> KotlinIr.IrCall.Primitive.UNARY
        is IrBinaryPrimitiveImpl
        -> KotlinIr.IrCall.Primitive.BINARY
        else
        -> KotlinIr.IrCall.Primitive.NOT_PRIMITIVE
    }

    private fun serializeMemberAccessCommon(call: IrMemberAccessExpression): KotlinIr.MemberAccessCommon {
        val proto = KotlinIr.MemberAccessCommon.newBuilder()
        if (call.extensionReceiver != null) {
            proto.extensionReceiver = serializeExpression(call.extensionReceiver!!)
        }

        if (call.dispatchReceiver != null) {
            proto.dispatchReceiver = serializeExpression(call.dispatchReceiver!!)
        }
        proto.typeArguments = serializeTypeArguments(call)

        for (index in 0..call.valueArgumentsCount - 1) {
            val actual = call.getValueArgument(index)
            val argOrNull = KotlinIr.NullableIrExpression.newBuilder()
            if (actual == null) {
                // Am I observing an IR generation regression?
                // I see a lack of arg for an empty vararg,
                // rather than an empty vararg node.

                // TODO: how do we assert that without descriptora?
                //assert(it.varargElementType != null || it.hasDefaultValue())
            } else {
                argOrNull.expression = serializeExpression(actual)
            }
            proto.addValueArgument(argOrNull)
        }
        return proto.build()
    }

    private fun serializeCall(call: IrCall): KotlinIr.IrCall {
        val proto = KotlinIr.IrCall.newBuilder()

        proto.kind = irCallToPrimitiveKind(call)
        proto.symbol = serializeIrSymbol(call.symbol)

        call.superQualifierSymbol?.let {
            proto.`super` = serializeIrSymbol(it)
        }
        proto.memberAccess = serializeMemberAccessCommon(call)
        return proto.build()
    }

    private fun serializeFunctionReference(callable: IrFunctionReference): KotlinIr.IrFunctionReference {
        val proto = KotlinIr.IrFunctionReference.newBuilder()
            .setSymbol(serializeIrSymbol(callable.symbol))
            .setMemberAccess(serializeMemberAccessCommon(callable))
        callable.origin?.let { proto.origin = serializeIrStatementOrigin(it) }
        return proto.build()
    }


    private fun serializePropertyReference(callable: IrPropertyReference): KotlinIr.IrPropertyReference {
        val proto = KotlinIr.IrPropertyReference.newBuilder()
            .setMemberAccess(serializeMemberAccessCommon(callable))
        callable.field?.let { proto.field = serializeIrSymbol(it) }
        callable.getter?.let { proto.getter = serializeIrSymbol(it) }
        callable.setter?.let { proto.setter = serializeIrSymbol(it) }
        callable.origin?.let { proto.origin = serializeIrStatementOrigin(it) }
        val property = callable.getter!!.owner.correspondingProperty!!
        descriptorReferenceSerializer.serializeDescriptorReference(property)?.let { proto.setDescriptorReference(it) }
        return proto.build()
    }

    private fun serializeClassReference(expression: IrClassReference): KotlinIr.IrClassReference {
        val proto = KotlinIr.IrClassReference.newBuilder()
            .setClassSymbol(serializeIrSymbol(expression.symbol))
            .setClassType(serializeIrType(expression.classType))
        return proto.build()
    }

    private fun serializeConst(value: IrConst<*>): KotlinIr.IrConst {
        val proto = KotlinIr.IrConst.newBuilder()
        when (value.kind) {
            IrConstKind.Null -> proto.`null` = true
            IrConstKind.Boolean -> proto.boolean = value.value as Boolean
            IrConstKind.Byte -> proto.byte = (value.value as Byte).toInt()
            IrConstKind.Char -> proto.char = (value.value as Char).toInt()
            IrConstKind.Short -> proto.short = (value.value as Short).toInt()
            IrConstKind.Int -> proto.int = value.value as Int
            IrConstKind.Long -> proto.long = value.value as Long
            IrConstKind.String -> proto.string = serializeString(value.value as String)
            IrConstKind.Float -> proto.float = value.value as Float
            IrConstKind.Double -> proto.double = value.value as Double
        }
        return proto.build()
    }

    private fun serializeDelegatingConstructorCall(call: IrDelegatingConstructorCall): KotlinIr.IrDelegatingConstructorCall {
        val proto = KotlinIr.IrDelegatingConstructorCall.newBuilder()
            .setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeDoWhile(expression: IrDoWhileLoop): KotlinIr.IrDoWhile {
        val proto = KotlinIr.IrDoWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    fun serializeEnumConstructorCall(call: IrEnumConstructorCall): KotlinIr.IrEnumConstructorCall {
        val proto = KotlinIr.IrEnumConstructorCall.newBuilder()
            .setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeGetClass(expression: IrGetClass): KotlinIr.IrGetClass {
        val proto = KotlinIr.IrGetClass.newBuilder()
            .setArgument(serializeExpression(expression.argument))
        return proto.build()
    }

    private fun serializeGetEnumValue(expression: IrGetEnumValue): KotlinIr.IrGetEnumValue {
        val proto = KotlinIr.IrGetEnumValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeFieldAccessCommon(expression: IrFieldAccessExpression): KotlinIr.FieldAccessCommon {
        val proto = KotlinIr.FieldAccessCommon.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        expression.superQualifierSymbol?.let { proto.`super` = serializeIrSymbol(it) }
        expression.receiver?.let { proto.receiver = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeGetField(expression: IrGetField): KotlinIr.IrGetField {
        val proto = KotlinIr.IrGetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
        return proto.build()
    }

    private fun serializeGetValue(expression: IrGetValue): KotlinIr.IrGetValue {
        val proto = KotlinIr.IrGetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeGetObject(expression: IrGetObjectValue): KotlinIr.IrGetObject {
        val proto = KotlinIr.IrGetObject.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeInstanceInitializerCall(call: IrInstanceInitializerCall): KotlinIr.IrInstanceInitializerCall {
        val proto = KotlinIr.IrInstanceInitializerCall.newBuilder()

        proto.symbol = serializeIrSymbol(call.classSymbol)

        return proto.build()
    }

    private fun serializeReturn(expression: IrReturn): KotlinIr.IrReturn {
        val proto = KotlinIr.IrReturn.newBuilder()
            .setReturnTarget(serializeIrSymbol(expression.returnTargetSymbol))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetField(expression: IrSetField): KotlinIr.IrSetField {
        val proto = KotlinIr.IrSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetVariable(expression: IrSetVariable): KotlinIr.IrSetVariable {
        val proto = KotlinIr.IrSetVariable.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSpreadElement(element: IrSpreadElement): KotlinIr.IrSpreadElement {
        val coordinates = serializeCoordinates(element.startOffset, element.endOffset)
        return KotlinIr.IrSpreadElement.newBuilder()
            .setExpression(serializeExpression(element.expression))
            .setCoordinates(coordinates)
            .build()
    }

    private fun serializeSyntheticBody(expression: IrSyntheticBody) = KotlinIr.IrSyntheticBody.newBuilder()
        .setKind(
            when (expression.kind) {
                IrSyntheticBodyKind.ENUM_VALUES -> KotlinIr.IrSyntheticBodyKind.ENUM_VALUES
                IrSyntheticBodyKind.ENUM_VALUEOF -> KotlinIr.IrSyntheticBodyKind.ENUM_VALUEOF
            }
        )
        .build()

    private fun serializeThrow(expression: IrThrow): KotlinIr.IrThrow {
        val proto = KotlinIr.IrThrow.newBuilder()
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeTry(expression: IrTry): KotlinIr.IrTry {
        val proto = KotlinIr.IrTry.newBuilder()
            .setResult(serializeExpression(expression.tryResult))
        val catchList = expression.catches
        catchList.forEach {
            proto.addCatch(serializeStatement(it))
        }
        val finallyExpression = expression.finallyExpression
        if (finallyExpression != null) {
            proto.finally = serializeExpression(finallyExpression)
        }
        return proto.build()
    }

    private fun serializeTypeOperator(operator: IrTypeOperator): KotlinIr.IrTypeOperator = when (operator) {
        IrTypeOperator.CAST
        -> KotlinIr.IrTypeOperator.CAST
        IrTypeOperator.IMPLICIT_CAST
        -> KotlinIr.IrTypeOperator.IMPLICIT_CAST
        IrTypeOperator.IMPLICIT_NOTNULL
        -> KotlinIr.IrTypeOperator.IMPLICIT_NOTNULL
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        -> KotlinIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        IrTypeOperator.IMPLICIT_INTEGER_COERCION
        -> KotlinIr.IrTypeOperator.IMPLICIT_INTEGER_COERCION
        IrTypeOperator.SAFE_CAST
        -> KotlinIr.IrTypeOperator.SAFE_CAST
        IrTypeOperator.INSTANCEOF
        -> KotlinIr.IrTypeOperator.INSTANCEOF
        IrTypeOperator.NOT_INSTANCEOF
        -> KotlinIr.IrTypeOperator.NOT_INSTANCEOF
        IrTypeOperator.SAM_CONVERSION
        -> KotlinIr.IrTypeOperator.SAM_CONVERSION
    }

    private fun serializeTypeOp(expression: IrTypeOperatorCall): KotlinIr.IrTypeOp {
        val proto = KotlinIr.IrTypeOp.newBuilder()
            .setOperator(serializeTypeOperator(expression.operator))
            .setOperand(serializeIrType(expression.typeOperand))
            .setArgument(serializeExpression(expression.argument))
        return proto.build()

    }

    private fun serializeVararg(expression: IrVararg): KotlinIr.IrVararg {
        val proto = KotlinIr.IrVararg.newBuilder()
            .setElementType(serializeIrType(expression.varargElementType))
        expression.elements.forEach {
            proto.addElement(serializeVarargElement(it))
        }
        return proto.build()
    }

    private fun serializeVarargElement(element: IrVarargElement): KotlinIr.IrVarargElement {
        val proto = KotlinIr.IrVarargElement.newBuilder()
        when (element) {
            is IrExpression
            -> proto.expression = serializeExpression(element)
            is IrSpreadElement
            -> proto.spreadElement = serializeSpreadElement(element)
            else -> error("Unknown vararg element kind")
        }
        return proto.build()
    }

    private fun serializeWhen(expression: IrWhen): KotlinIr.IrWhen {
        val proto = KotlinIr.IrWhen.newBuilder()

        val branches = expression.branches
        branches.forEach {
            proto.addBranch(serializeStatement(it))
        }

        return proto.build()
    }

    private fun serializeLoop(expression: IrLoop): KotlinIr.Loop {
        val proto = KotlinIr.Loop.newBuilder()
            .setCondition(serializeExpression(expression.condition))
        val label = expression.label?.let { serializeString(it) }
        if (label != null) {
            proto.label = label
        }

        proto.loopId = currentLoopIndex
        loopIndex[expression] = currentLoopIndex++

        val body = expression.body
        if (body != null) {
            proto.body = serializeExpression(body)
        }

        return proto.build()
    }

    private fun serializeWhile(expression: IrWhileLoop): KotlinIr.IrWhile {
        val proto = KotlinIr.IrWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    private fun serializeDynamicMemberExpression(expression: IrDynamicMemberExpression): KotlinIr.IrDynamicMemberExpression {
        val proto = KotlinIr.IrDynamicMemberExpression.newBuilder()
            .setMemberName(serializeString(expression.memberName))
            .setReceiver(serializeExpression(expression.receiver))

        return proto.build()
    }

    private fun serializeDynamicOperatorExpression(expression: IrDynamicOperatorExpression): KotlinIr.IrDynamicOperatorExpression {
        val proto = KotlinIr.IrDynamicOperatorExpression.newBuilder()
            .setOperator(serializeDynamicOperator(expression.operator))
            .setReceiver(serializeExpression(expression.receiver))

        expression.arguments.forEach { proto.addArgument(serializeExpression(it)) }

        return proto.build()
    }

    private fun serializeDynamicOperator(operator: IrDynamicOperator) = when (operator) {
        IrDynamicOperator.UNARY_PLUS -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.UNARY_PLUS
        IrDynamicOperator.UNARY_MINUS -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.UNARY_MINUS

        IrDynamicOperator.EXCL -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EXCL

        IrDynamicOperator.PREFIX_INCREMENT -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.PREFIX_INCREMENT
        IrDynamicOperator.PREFIX_DECREMENT -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.PREFIX_DECREMENT

        IrDynamicOperator.POSTFIX_INCREMENT -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.POSTFIX_INCREMENT
        IrDynamicOperator.POSTFIX_DECREMENT -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.POSTFIX_DECREMENT

        IrDynamicOperator.BINARY_PLUS -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.BINARY_PLUS
        IrDynamicOperator.BINARY_MINUS -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.BINARY_MINUS
        IrDynamicOperator.MUL -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MUL
        IrDynamicOperator.DIV -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.DIV
        IrDynamicOperator.MOD -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MOD

        IrDynamicOperator.GT -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.GT
        IrDynamicOperator.LT -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.LT
        IrDynamicOperator.GE -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.GE
        IrDynamicOperator.LE -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.LE

        IrDynamicOperator.EQEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EQEQ
        IrDynamicOperator.EXCLEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EXCLEQ

        IrDynamicOperator.EQEQEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EQEQEQ
        IrDynamicOperator.EXCLEQEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EXCLEQEQ

        IrDynamicOperator.ANDAND -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.ANDAND
        IrDynamicOperator.OROR -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.OROR

        IrDynamicOperator.EQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.EQ
        IrDynamicOperator.PLUSEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.PLUSEQ
        IrDynamicOperator.MINUSEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MINUSEQ
        IrDynamicOperator.MULEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MULEQ
        IrDynamicOperator.DIVEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.DIVEQ
        IrDynamicOperator.MODEQ -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.MODEQ

        IrDynamicOperator.ARRAY_ACCESS -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.ARRAY_ACCESS

        IrDynamicOperator.INVOKE -> KotlinIr.IrDynamicOperatorExpression.IrDynamicOperator.INVOKE
    }

    private fun serializeBreak(expression: IrBreak): KotlinIr.IrBreak {
        val proto = KotlinIr.IrBreak.newBuilder()
        val label = expression.label?.let { serializeString(it) }
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeContinue(expression: IrContinue): KotlinIr.IrContinue {
        val proto = KotlinIr.IrContinue.newBuilder()
        val label = expression.label?.let { serializeString(it) }
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeExpression(expression: IrExpression): KotlinIr.IrExpression {
        logger.log { "### serializing Expression: ${ir2string(expression)}" }

        val coordinates = serializeCoordinates(expression.startOffset, expression.endOffset)
        val proto = KotlinIr.IrExpression.newBuilder()
            .setType(serializeIrType(expression.type))
            .setCoordinates(coordinates)

        val operationProto = KotlinIr.IrOperation.newBuilder()

        // TODO: make me a visitor.
        when (expression) {
            is IrBlock -> operationProto.block = serializeBlock(expression)
            is IrBreak -> operationProto.`break` = serializeBreak(expression)
            is IrClassReference
            -> operationProto.classReference = serializeClassReference(expression)
            is IrCall -> operationProto.call = serializeCall(expression)

            is IrComposite -> operationProto.composite = serializeComposite(expression)
            is IrConst<*> -> operationProto.const = serializeConst(expression)
            is IrContinue -> operationProto.`continue` = serializeContinue(expression)
            is IrDelegatingConstructorCall
            -> operationProto.delegatingConstructorCall = serializeDelegatingConstructorCall(expression)
            is IrDoWhileLoop -> operationProto.doWhile = serializeDoWhile(expression)
            is IrEnumConstructorCall
            -> operationProto.enumConstructorCall = serializeEnumConstructorCall(expression)
            is IrFunctionReference
            -> operationProto.functionReference = serializeFunctionReference(expression)
            is IrGetClass -> operationProto.getClass = serializeGetClass(expression)
            is IrGetField -> operationProto.getField = serializeGetField(expression)
            is IrGetValue -> operationProto.getValue = serializeGetValue(expression)
            is IrGetEnumValue
            -> operationProto.getEnumValue = serializeGetEnumValue(expression)
            is IrGetObjectValue
            -> operationProto.getObject = serializeGetObject(expression)
            is IrInstanceInitializerCall
            -> operationProto.instanceInitializerCall = serializeInstanceInitializerCall(expression)
            is IrPropertyReference
            -> operationProto.propertyReference = serializePropertyReference(expression)
            is IrReturn -> operationProto.`return` = serializeReturn(expression)
            is IrSetField -> operationProto.setField = serializeSetField(expression)
            is IrSetVariable -> operationProto.setVariable = serializeSetVariable(expression)
            is IrStringConcatenation
            -> operationProto.stringConcat = serializeStringConcat(expression)
            is IrThrow -> operationProto.`throw` = serializeThrow(expression)
            is IrTry -> operationProto.`try` = serializeTry(expression)
            is IrTypeOperatorCall
            -> operationProto.typeOp = serializeTypeOp(expression)
            is IrVararg -> operationProto.vararg = serializeVararg(expression)
            is IrWhen -> operationProto.`when` = serializeWhen(expression)
            is IrWhileLoop -> operationProto.`while` = serializeWhile(expression)
            is IrDynamicMemberExpression -> operationProto.dynamicMember = serializeDynamicMemberExpression(expression)
            is IrDynamicOperatorExpression -> operationProto.dynamicOperator = serializeDynamicOperatorExpression(expression)
            else -> {
                TODO("Expression serialization not implemented yet: ${ir2string(expression)}.")
            }
        }
        proto.setOperation(operationProto)

        return proto.build()
    }

    private fun serializeStatement(statement: IrElement): KotlinIr.IrStatement {
        logger.log { "### serializing Statement: ${ir2string(statement)}" }

        val coordinates = serializeCoordinates(statement.startOffset, statement.endOffset)
        val proto = KotlinIr.IrStatement.newBuilder()
            .setCoordinates(coordinates)

        when (statement) {
            is IrDeclaration -> {
                logger.log { " ###Declaration " }; proto.declaration = serializeDeclaration(statement)
            }
            is IrExpression -> {
                logger.log { " ###Expression " }; proto.expression = serializeExpression(statement)
            }
            is IrBlockBody -> {
                logger.log { " ###BlockBody " }; proto.blockBody = serializeBlockBody(statement)
            }
            is IrBranch -> {
                logger.log { " ###Branch " }; proto.branch = serializeBranch(statement)
            }
            is IrCatch -> {
                logger.log { " ###Catch " }; proto.catch = serializeCatch(statement)
            }
            is IrSyntheticBody -> {
                logger.log { " ###SyntheticBody " }; proto.syntheticBody = serializeSyntheticBody(statement)
            }
            else -> {
                TODO("Statement not implemented yet: ${ir2string(statement)}")
            }
        }
        return proto.build()
    }

    private fun serializeIrValueParameter(parameter: IrValueParameter): KotlinIr.IrValueParameter {
        val proto = KotlinIr.IrValueParameter.newBuilder()
            .setSymbol(serializeIrSymbol(parameter.symbol))
            .setName(serializeName(parameter.name))
            .setIndex(parameter.index)
            .setType(serializeIrType(parameter.type))
            .setIsCrossinline(parameter.isCrossinline)
            .setIsNoinline(parameter.isNoinline)

        parameter.varargElementType?.let { proto.setVarargElementType(serializeIrType(it)) }
        parameter.defaultValue?.let { proto.setDefaultValue(serializeExpression(it.expression)) }

        return proto.build()
    }

    private fun serializeIrTypeParameter(parameter: IrTypeParameter): KotlinIr.IrTypeParameter {
        val proto = KotlinIr.IrTypeParameter.newBuilder()
            .setSymbol(serializeIrSymbol(parameter.symbol))
            .setName(serializeName(parameter.name))
            .setIndex(parameter.index)
            .setVariance(serializeIrTypeVariance(parameter.variance))
            .setIsReified(parameter.isReified)
        parameter.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }
        return proto.build()
    }

    private fun serializeIrTypeParameterContainer(typeParameters: List<IrTypeParameter>): KotlinIr.IrTypeParameterContainer {
        val proto = KotlinIr.IrTypeParameterContainer.newBuilder()
        typeParameters.forEach {
            proto.addTypeParameter(serializeDeclaration(it))
        }
        return proto.build()
    }

    private fun serializeIrFunctionBase(function: IrFunction): KotlinIr.IrFunctionBase {
        val proto = KotlinIr.IrFunctionBase.newBuilder()
            .setName(serializeName(function.name))
            .setVisibility(serializeVisibility(function.visibility))
            .setIsInline(function.isInline)
            .setIsExternal(function.isExternal)
            .setReturnType(serializeIrType(function.returnType))
            .setTypeParameters(serializeIrTypeParameterContainer(function.typeParameters))

        function.dispatchReceiverParameter?.let { proto.setDispatchReceiver(serializeDeclaration(it)) }
        function.extensionReceiverParameter?.let { proto.setExtensionReceiver(serializeDeclaration(it)) }
        function.valueParameters.forEach {
            //proto.addValueParameter(serializeIrValueParameter(it))
            proto.addValueParameter(serializeDeclaration(it))
        }
        if (!bodiesOnlyForInlines || function.isInline) {
            function.body?.let { proto.body = serializeStatement(it) }
        }
        return proto.build()
    }

    private fun serializeModality(modality: Modality) = when (modality) {
        Modality.FINAL -> KotlinIr.ModalityKind.FINAL_MODALITY
        Modality.SEALED -> KotlinIr.ModalityKind.SEALED_MODALITY
        Modality.OPEN -> KotlinIr.ModalityKind.OPEN_MODALITY
        Modality.ABSTRACT -> KotlinIr.ModalityKind.ABSTRACT_MODALITY
    }

    private fun serializeIrConstructor(declaration: IrConstructor): KotlinIr.IrConstructor =
        KotlinIr.IrConstructor.newBuilder()
            .setSymbol(serializeIrSymbol(declaration.symbol))
            .setBase(serializeIrFunctionBase(declaration))
            .setIsPrimary(declaration.isPrimary)
            .build()

    private fun serializeIrFunction(declaration: IrSimpleFunction): KotlinIr.IrFunction {
        val function = declaration// as IrFunctionImpl
        val proto = KotlinIr.IrFunction.newBuilder()
            .setSymbol(serializeIrSymbol(function.symbol))
            .setModality(serializeModality(function.modality))
            .setIsTailrec(function.isTailrec)
            .setIsSuspend(function.isSuspend)

        function.overriddenSymbols.forEach {
            proto.addOverridden(serializeIrSymbol(it))
        }

        //function.correspondingProperty?.let {
        //    val uniqId = declarationTable.uniqIdByDeclaration(it)
        //    proto.setCorrespondingProperty(protoUniqId(uniqId))
        //}

        val base = serializeIrFunctionBase(function)
        proto.setBase(base)

        return proto.build()
    }

    private fun serializeIrAnonymousInit(declaration: IrAnonymousInitializer) = KotlinIr.IrAnonymousInit.newBuilder()
        .setSymbol(serializeIrSymbol(declaration.symbol))
        .setBody(serializeStatement(declaration.body))
        .build()

    private fun serializeIrProperty(property: IrProperty): KotlinIr.IrProperty {
        val index = declarationTable.uniqIdByDeclaration(property)

        val proto = KotlinIr.IrProperty.newBuilder()
            .setIsDelegated(property.isDelegated)
            .setName(serializeName(property.name))
            .setVisibility(serializeVisibility(property.visibility))
            .setModality(serializeModality(property.modality))
            .setIsVar(property.isVar)
            .setIsConst(property.isConst)
            .setIsLateinit(property.isLateinit)
            .setIsDelegated(property.isDelegated)
            .setIsExternal(property.isExternal)

        descriptorReferenceSerializer.serializeDescriptorReference(property)?.let { proto.setDescriptorReference(it) }

        val backingField = property.backingField
        val getter = property.getter
        val setter = property.setter
        if (backingField != null)
            proto.backingField = serializeIrField(backingField)
        if (getter != null)
            proto.getter = serializeIrFunction(getter)
        if (setter != null)
            proto.setter = serializeIrFunction(setter)

        return proto.build()
    }

    private fun serializeIrField(field: IrField): KotlinIr.IrField {
        val proto = KotlinIr.IrField.newBuilder()
            .setSymbol(serializeIrSymbol(field.symbol))
            .setName(serializeName(field.name))
            .setVisibility(serializeVisibility(field.visibility))
            .setIsFinal(field.isFinal)
            .setIsExternal(field.isExternal)
            .setIsStatic(field.isStatic)
            .setType(serializeIrType(field.type))
        val initializer = field.initializer?.expression
        if (initializer != null) {
            proto.initializer = serializeExpression(initializer)
        }
        return proto.build()
    }

    private fun serializeIrVariable(variable: IrVariable): KotlinIr.IrVariable {
        val proto = KotlinIr.IrVariable.newBuilder()
            .setSymbol(serializeIrSymbol(variable.symbol))
            .setName(serializeName(variable.name))
            .setType(serializeIrType(variable.type))
            .setIsConst(variable.isConst)
            .setIsVar(variable.isVar)
            .setIsLateinit(variable.isLateinit)
        variable.initializer?.let { proto.initializer = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeIrDeclarationContainer(declarations: List<IrDeclaration>): KotlinIr.IrDeclarationContainer {
        val proto = KotlinIr.IrDeclarationContainer.newBuilder()
        declarations.forEach {
            //if (it is IrDeclarationWithVisibility && it.visibility == Visibilities.INVISIBLE_FAKE) return@forEach
            proto.addDeclaration(serializeDeclaration(it))
        }
        return proto.build()
    }

    private fun serializeClassKind(kind: ClassKind) = when (kind) {
        CLASS -> KotlinIr.ClassKind.CLASS
        INTERFACE -> KotlinIr.ClassKind.INTERFACE
        ENUM_CLASS -> KotlinIr.ClassKind.ENUM_CLASS
        ENUM_ENTRY -> KotlinIr.ClassKind.ENUM_ENTRY
        ANNOTATION_CLASS -> KotlinIr.ClassKind.ANNOTATION_CLASS
        OBJECT -> KotlinIr.ClassKind.OBJECT
    }

    private fun serializeIrClass(clazz: IrClass): KotlinIr.IrClass {
        val proto = KotlinIr.IrClass.newBuilder()
            .setName(serializeName(clazz.name))
            .setSymbol(serializeIrSymbol(clazz.symbol))
            .setKind(serializeClassKind(clazz.kind))
            .setVisibility(serializeVisibility(clazz.visibility))
            .setModality(serializeModality(clazz.modality))
            .setIsCompanion(clazz.isCompanion)
            .setIsInner(clazz.isInner)
            .setIsData(clazz.isData)
            .setIsExternal(clazz.isExternal)
            .setIsInline(clazz.isInline)
            .setTypeParameters(serializeIrTypeParameterContainer(clazz.typeParameters))
            .setDeclarationContainer(serializeIrDeclarationContainer(clazz.declarations))
        clazz.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }
        clazz.thisReceiver?.let { proto.thisReceiver = serializeDeclaration(it) }

        return proto.build()
    }

    private fun serializeIrEnumEntry(enumEntry: IrEnumEntry): KotlinIr.IrEnumEntry {
        val proto = KotlinIr.IrEnumEntry.newBuilder()
            .setName(serializeName(enumEntry.name))
            .setSymbol(serializeIrSymbol(enumEntry.symbol))

        enumEntry.initializerExpression?.let {
            proto.initializer = serializeExpression(it)
        }
        enumEntry.correspondingClass?.let {
            proto.correspondingClass = serializeDeclaration(it)
        }
        return proto.build()
    }

    private fun serializeDeclaration(declaration: IrDeclaration): KotlinIr.IrDeclaration {
        logger.log { "### serializing Declaration: ${ir2string(declaration)}" }

        val declarator = KotlinIr.IrDeclarator.newBuilder()

        when (declaration) {
            is IrAnonymousInitializer ->
                declarator.irAnonymousInit = serializeIrAnonymousInit(declaration)
            is IrConstructor ->
                declarator.irConstructor = serializeIrConstructor(declaration)
            is IrField ->
                declarator.irField = serializeIrField(declaration)
            is IrSimpleFunction ->
                declarator.irFunction = serializeIrFunction(declaration)
            is IrTypeParameter ->
                declarator.irTypeParameter = serializeIrTypeParameter(declaration)
            is IrVariable ->
                declarator.irVariable = serializeIrVariable(declaration)
            is IrValueParameter ->
                declarator.irValueParameter = serializeIrValueParameter(declaration)
            is IrClass ->
                declarator.irClass = serializeIrClass(declaration)
            is IrEnumEntry ->
                declarator.irEnumEntry = serializeIrEnumEntry(declaration)
            is IrProperty ->
                declarator.irProperty = serializeIrProperty(declaration)
            else
            -> TODO("Declaration serialization not supported yet: $declaration")
        }

        val coordinates = serializeCoordinates(declaration.startOffset, declaration.endOffset)
        val annotations = serializeAnnotations(declaration.annotations)
        val origin = serializeIrDeclarationOrigin(declaration.origin)
        val proto = KotlinIr.IrDeclaration.newBuilder()
            .setCoordinates(coordinates)
            .setAnnotations(annotations)
            .setOrigin(origin)


        proto.setDeclarator(declarator)

        return proto.build()
    }

// ---------- Top level ------------------------------------------------------

    fun serializeFileEntry(entry: SourceManager.FileEntry) = KotlinIr.FileEntry.newBuilder()
        .setName(serializeString(entry.name))
        .addAllLineStartOffsets(entry.lineStartOffsets.asIterable())
        .build()

    open fun backendSpecificExplicitRoot(declaration: IrFunction) = false
    open fun backendSpecificExplicitRoot(declaration: IrClass) = false

    fun serializeIrFile(file: IrFile): KotlinIr.IrFile {
        val proto = KotlinIr.IrFile.newBuilder()
            .setFileEntry(serializeFileEntry(file.fileEntry))
            .setFqName(serializeString(file.fqName.toString()))
            .setAnnotations(serializeAnnotations(file.annotations))

        file.declarations.forEach {
            if (it.descriptor.isExpectMember && !it.descriptor.isSerializableExpectClass) {
                writer.skipDeclaration()
                return@forEach
            }

            val byteArray = serializeDeclaration(it).toByteArray()
            val uniqId = declarationTable.uniqIdByDeclaration(it)
            writer.addDeclaration(DeclarationId(uniqId.index, uniqId.isLocal), byteArray)
            proto.addDeclarationId(protoUniqId(uniqId))
        }

        // TODO: is it Konan specific?

        // Make sure that all top level properties are initialized on library's load.
        file.declarations
                .filterIsInstance<IrProperty>()
                .filter { it.backingField?.initializer != null && !it.isConst }
                .forEach { proto.addExplicitlyExportedToCompiler(serializeIrSymbol(it.backingField!!.symbol)) }

        // TODO: Konan specific

        file.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                if (backendSpecificExplicitRoot(declaration)) {
                    proto.addExplicitlyExportedToCompiler(serializeIrSymbol(declaration.symbol))
                }
                super.visitDeclaration(declaration)
            }

            override fun visitClass(declaration: IrClass) {
                if (backendSpecificExplicitRoot(declaration)) {
                    proto.addExplicitlyExportedToCompiler(serializeIrSymbol(declaration.symbol))
                }
                super.visitDeclaration(declaration)
            }
        })

        return proto.build()
    }

    lateinit var writer: CombinedIrFileWriter

    fun serializeModule(module: IrModuleFragment): KotlinIr.IrModule {
        val proto = KotlinIr.IrModule.newBuilder()
            .setName(serializeName(module.name))

        val topLevelDeclarationsCount = module.files.sumBy { it.declarations.size }

        writer = CombinedIrFileWriter(topLevelDeclarationsCount)

        module.files.forEach {
            proto.addFile(serializeIrFile(it))
        }
        proto.symbolTable = KotlinIr.IrSymbolTable.newBuilder()
            .addAllSymbols(protoSymbolArray)
            .build()

        proto.typeTable = KotlinIr.IrTypeTable.newBuilder()
            .addAllTypes(protoTypeArray)
            .build()

        proto.stringTable = KotlinIr.StringTable.newBuilder()
            .addAllStrings(protoStringArray)
            .build()

        return proto.build()
    }

    fun serializedIrModule(module: IrModuleFragment): SerializedIr {
        val moduleHeader = serializeModule(module).toByteArray()
        return SerializedIr(moduleHeader, writer.finishWriting().absolutePath)
    }
}
