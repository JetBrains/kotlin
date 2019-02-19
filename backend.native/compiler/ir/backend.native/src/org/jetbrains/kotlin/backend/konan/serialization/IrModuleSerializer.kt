/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.findTopLevelDeclaration
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.backend.konan.descriptors.isSerializableExpectClass
import org.jetbrains.kotlin.backend.konan.ir.lineStartOffsets
import org.jetbrains.kotlin.backend.konan.library.SerializedIr
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrNullaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrUnaryPrimitiveImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.library.impl.CombinedIrFileWriter
import org.jetbrains.kotlin.konan.library.impl.DeclarationId
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.Variance

internal class IrModuleSerializer(
    val logger: LoggingContext,
    val declarationTable: DeclarationTable,
    val bodiesOnlyForInlines: Boolean = false
) {

    private val loopIndex = mutableMapOf<IrLoop, Int>()
    private var currentLoopIndex = 0
    val descriptorReferenceSerializer = DescriptorReferenceSerializer(declarationTable, { string -> serializeString(string) })

    // The same symbol can be used multiple times in a module
    // so use this index to store symbol data only once.
    val protoSymbolMap = mutableMapOf<IrSymbol, Int>()
    val protoSymbolArray = arrayListOf<KonanIr.IrSymbolData>()

    // The same type can be used multiple times in a module
    // so use this index to store type data only once.
    val protoTypeMap = mutableMapOf<IrTypeKey, Int>()
    val protoTypeArray = arrayListOf<KonanIr.IrType>()

    val protoStringMap = mutableMapOf<String, Int>()
    val protoStringArray = arrayListOf<String>()

    /* ------- Common fields ---------------------------------------------------- */

    private fun serializeIrDeclarationOrigin(origin: IrDeclarationOrigin) =
        KonanIr.IrDeclarationOrigin.newBuilder()
            .setCustom(serializeString((origin as IrDeclarationOriginImpl).name))
            .build()

    private fun serializeIrStatementOrigin(origin: IrStatementOrigin) =
        KonanIr.IrStatementOrigin.newBuilder()
            .setName(serializeString((origin as IrStatementOriginImpl).debugName))
            .build()

    private fun serializeVisibility(visibility: Visibility) =
        KonanIr.Visibility.newBuilder()
            .setName(serializeString(visibility.name))
            .build()

    private fun serializeCoordinates(start: Int, end: Int): KonanIr.Coordinates {
        return KonanIr.Coordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build()
    }

    /* ------- Strings ---------------------------------------------------------- */

    fun serializeString(value: String): KonanIr.String {
        val proto = KonanIr.String.newBuilder()
        proto.index = protoStringMap.getOrPut(value) {
            protoStringArray.add(value)
            protoStringArray.size - 1
        }
        return proto.build()
    }

    /* ------- IrSymbols -------------------------------------------------------- */

    fun protoSymbolKind(symbol: IrSymbol): KonanIr.IrSymbolKind = when (symbol) {
        is IrAnonymousInitializerSymbol ->
            KonanIr.IrSymbolKind.ANONYMOUS_INIT_SYMBOL
        is IrClassSymbol ->
            KonanIr.IrSymbolKind.CLASS_SYMBOL
        is IrConstructorSymbol ->
            KonanIr.IrSymbolKind.CONSTRUCTOR_SYMBOL
        is IrTypeParameterSymbol ->
            KonanIr.IrSymbolKind.TYPE_PARAMETER_SYMBOL
        is IrEnumEntrySymbol ->
            KonanIr.IrSymbolKind.ENUM_ENTRY_SYMBOL
        is IrVariableSymbol ->
            KonanIr.IrSymbolKind.VARIABLE_SYMBOL
        is IrValueParameterSymbol ->
            if (symbol.descriptor is ReceiverParameterDescriptor) // TODO: we use descriptor here.
                KonanIr.IrSymbolKind.RECEIVER_PARAMETER_SYMBOL
            else
                KonanIr.IrSymbolKind.VALUE_PARAMETER_SYMBOL
        is IrSimpleFunctionSymbol ->
            KonanIr.IrSymbolKind.FUNCTION_SYMBOL
        is IrReturnableBlockSymbol ->
            KonanIr.IrSymbolKind.RETURNABLE_BLOCK_SYMBOL
        is IrFieldSymbol ->
            if (symbol.owner.correspondingProperty.let { it == null || it.isDelegated })
                KonanIr.IrSymbolKind.STANDALONE_FIELD_SYMBOL
            else
                KonanIr.IrSymbolKind.FIELD_SYMBOL
        else ->
            TODO("Unexpected symbol kind: $symbol")
    }

    fun serializeIrSymbolData(symbol: IrSymbol): KonanIr.IrSymbolData {

        val declaration = symbol.owner as? IrDeclaration ?: error("Expected IrDeclaration: ${symbol.owner}")

        val proto = KonanIr.IrSymbolData.newBuilder()
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

    fun serializeIrSymbol(symbol: IrSymbol): KonanIr.IrSymbol {
        val proto = KonanIr.IrSymbol.newBuilder()
        proto.index = protoSymbolMap.getOrPut(symbol) {
            protoSymbolArray.add(serializeIrSymbolData(symbol))
            protoSymbolArray.size - 1
        }
        return proto.build()
    }

    /* ------- IrTypes ---------------------------------------------------------- */

    // TODO: we, probably, need a type table.

    private fun serializeTypeArguments(call: IrMemberAccessExpression): KonanIr.TypeArguments {
        val proto = KonanIr.TypeArguments.newBuilder()
        for (i in 0 until call.typeArgumentsCount) {
            proto.addTypeArgument(serializeIrType(call.getTypeArgument(i)!!))
        }
        return proto.build()
    }

    fun serializeIrTypeVariance(variance: Variance) = when (variance) {
        Variance.IN_VARIANCE -> KonanIr.IrTypeVariance.IN
        Variance.OUT_VARIANCE -> KonanIr.IrTypeVariance.OUT
        Variance.INVARIANT -> KonanIr.IrTypeVariance.INV
    }

    fun serializeAnnotations(annotations: List<IrCall>): KonanIr.Annotations {
        val proto = KonanIr.Annotations.newBuilder()
        annotations.forEach {
            proto.addAnnotation(serializeCall(it))
        }
        return proto.build()
    }

    fun serializeIrTypeProjection(argument: IrTypeProjection) = KonanIr.IrTypeProjection.newBuilder()
        .setVariance(serializeIrTypeVariance(argument.variance))
        .setType(serializeIrType(argument.type))
        .build()

    fun serializeTypeArgument(argument: IrTypeArgument): KonanIr.IrTypeArgument {
        val proto = KonanIr.IrTypeArgument.newBuilder()
        when (argument) {
            is IrStarProjection ->
                proto.star = KonanIr.IrStarProjection.newBuilder()
                    .build() // TODO: Do we need a singletone here? Or just an enum?
            is IrTypeProjection ->
                proto.type = serializeIrTypeProjection(argument)
            else -> TODO("Unexpected type argument kind: $argument")
        }
        return proto.build()
    }

    fun serializeSimpleType(type: IrSimpleType): KonanIr.IrSimpleType {
        val proto = KonanIr.IrSimpleType.newBuilder()
            .setAnnotations(serializeAnnotations(type.annotations))
            .setClassifier(serializeIrSymbol(type.classifier))
            .setHasQuestionMark(type.hasQuestionMark)
        type.arguments.forEach {
            proto.addArgument(serializeTypeArgument(it))
        }
        return proto.build()
    }

    fun serializeDynamicType(type: IrDynamicType) = KonanIr.IrDynamicType.newBuilder()
        .setAnnotations(serializeAnnotations(type.annotations))
        .build()

    fun serializeErrorType(type: IrErrorType) = KonanIr.IrErrorType.newBuilder()
        .setAnnotations(serializeAnnotations(type.annotations))
        .build()

    private fun serializeIrTypeData(type: IrType): KonanIr.IrType {
        logger.log { "### serializing IrType: " + type }
        val proto = KonanIr.IrType.newBuilder()
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

    fun serializeIrType(type: IrType): KonanIr.IrTypeIndex {
        val proto = KonanIr.IrTypeIndex.newBuilder()
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

    private fun serializeBlockBody(expression: IrBlockBody): KonanIr.IrBlockBody {
        val proto = KonanIr.IrBlockBody.newBuilder()
        expression.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeBranch(branch: IrBranch): KonanIr.IrBranch {
        val proto = KonanIr.IrBranch.newBuilder()

        proto.condition = serializeExpression(branch.condition)
        proto.result = serializeExpression(branch.result)

        return proto.build()
    }

    private fun serializeBlock(block: IrBlock): KonanIr.IrBlock {
        val isLambdaOrigin =
            block.origin == IrStatementOrigin.LAMBDA ||
                    block.origin == IrStatementOrigin.ANONYMOUS_FUNCTION
        val proto = KonanIr.IrBlock.newBuilder()
            .setIsLambdaOrigin(isLambdaOrigin)
        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeComposite(composite: IrComposite): KonanIr.IrComposite {
        val proto = KonanIr.IrComposite.newBuilder()
        composite.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeCatch(catch: IrCatch): KonanIr.IrCatch {
        val proto = KonanIr.IrCatch.newBuilder()
            .setCatchParameter(serializeDeclaration(catch.catchParameter))
            .setResult(serializeExpression(catch.result))
        return proto.build()
    }

    private fun serializeStringConcat(expression: IrStringConcatenation): KonanIr.IrStringConcat {
        val proto = KonanIr.IrStringConcat.newBuilder()
        expression.arguments.forEach {
            proto.addArgument(serializeExpression(it))
        }
        return proto.build()
    }

    private fun irCallToPrimitiveKind(call: IrCall): KonanIr.IrCall.Primitive = when (call) {
        is IrNullaryPrimitiveImpl
        -> KonanIr.IrCall.Primitive.NULLARY
        is IrUnaryPrimitiveImpl
        -> KonanIr.IrCall.Primitive.UNARY
        is IrBinaryPrimitiveImpl
        -> KonanIr.IrCall.Primitive.BINARY
        else
        -> KonanIr.IrCall.Primitive.NOT_PRIMITIVE
    }

    private fun serializeMemberAccessCommon(call: IrMemberAccessExpression): KonanIr.MemberAccessCommon {
        val proto = KonanIr.MemberAccessCommon.newBuilder()
        if (call.extensionReceiver != null) {
            proto.extensionReceiver = serializeExpression(call.extensionReceiver!!)
        }

        if (call.dispatchReceiver != null) {
            proto.dispatchReceiver = serializeExpression(call.dispatchReceiver!!)
        }
        proto.typeArguments = serializeTypeArguments(call)

        for (index in 0..call.valueArgumentsCount - 1) {
            val actual = call.getValueArgument(index)
            val argOrNull = KonanIr.NullableIrExpression.newBuilder()
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

    private fun serializeCall(call: IrCall): KonanIr.IrCall {
        val proto = KonanIr.IrCall.newBuilder()

        proto.kind = irCallToPrimitiveKind(call)
        proto.symbol = serializeIrSymbol(call.symbol)

        call.superQualifierSymbol?.let {
            proto.`super` = serializeIrSymbol(it)
        }
        proto.memberAccess = serializeMemberAccessCommon(call)
        return proto.build()
    }

    private fun serializeFunctionReference(callable: IrFunctionReference): KonanIr.IrFunctionReference {
        val proto = KonanIr.IrFunctionReference.newBuilder()
            .setSymbol(serializeIrSymbol(callable.symbol))
            .setMemberAccess(serializeMemberAccessCommon(callable))
        callable.origin?.let { proto.origin = serializeIrStatementOrigin(it) }
        return proto.build()
    }


    private fun serializePropertyReference(callable: IrPropertyReference): KonanIr.IrPropertyReference {
        val proto = KonanIr.IrPropertyReference.newBuilder()
            .setMemberAccess(serializeMemberAccessCommon(callable))
        callable.field?.let { proto.field = serializeIrSymbol(it) }
        callable.getter?.let { proto.getter = serializeIrSymbol(it) }
        callable.setter?.let { proto.setter = serializeIrSymbol(it) }
        callable.origin?.let { proto.origin = serializeIrStatementOrigin(it) }
        val property = callable.getter!!.owner.correspondingProperty!!
        descriptorReferenceSerializer.serializeDescriptorReference(property)?.let { proto.setDescriptor(it) }
        return proto.build()
    }

    private fun serializeClassReference(expression: IrClassReference): KonanIr.IrClassReference {
        val proto = KonanIr.IrClassReference.newBuilder()
            .setClassSymbol(serializeIrSymbol(expression.symbol))
            .setClassType(serializeIrType(expression.classType))
        return proto.build()
    }

    private fun serializeConst(value: IrConst<*>): KonanIr.IrConst {
        val proto = KonanIr.IrConst.newBuilder()
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

    private fun serializeDelegatingConstructorCall(call: IrDelegatingConstructorCall): KonanIr.IrDelegatingConstructorCall {
        val proto = KonanIr.IrDelegatingConstructorCall.newBuilder()
            .setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeDoWhile(expression: IrDoWhileLoop): KonanIr.IrDoWhile {
        val proto = KonanIr.IrDoWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    fun serializeEnumConstructorCall(call: IrEnumConstructorCall): KonanIr.IrEnumConstructorCall {
        val proto = KonanIr.IrEnumConstructorCall.newBuilder()
            .setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeGetClass(expression: IrGetClass): KonanIr.IrGetClass {
        val proto = KonanIr.IrGetClass.newBuilder()
            .setArgument(serializeExpression(expression.argument))
        return proto.build()
    }

    private fun serializeGetEnumValue(expression: IrGetEnumValue): KonanIr.IrGetEnumValue {
        val proto = KonanIr.IrGetEnumValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeFieldAccessCommon(expression: IrFieldAccessExpression): KonanIr.FieldAccessCommon {
        val proto = KonanIr.FieldAccessCommon.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        expression.superQualifierSymbol?.let { proto.`super` = serializeIrSymbol(it) }
        expression.receiver?.let { proto.receiver = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeGetField(expression: IrGetField): KonanIr.IrGetField {
        val proto = KonanIr.IrGetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
        return proto.build()
    }

    private fun serializeGetValue(expression: IrGetValue): KonanIr.IrGetValue {
        val proto = KonanIr.IrGetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeGetObject(expression: IrGetObjectValue): KonanIr.IrGetObject {
        val proto = KonanIr.IrGetObject.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeInstanceInitializerCall(call: IrInstanceInitializerCall): KonanIr.IrInstanceInitializerCall {
        val proto = KonanIr.IrInstanceInitializerCall.newBuilder()

        proto.symbol = serializeIrSymbol(call.classSymbol)

        return proto.build()
    }

    private fun serializeReturn(expression: IrReturn): KonanIr.IrReturn {
        val proto = KonanIr.IrReturn.newBuilder()
            .setReturnTarget(serializeIrSymbol(expression.returnTargetSymbol))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetField(expression: IrSetField): KonanIr.IrSetField {
        val proto = KonanIr.IrSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetVariable(expression: IrSetVariable): KonanIr.IrSetVariable {
        val proto = KonanIr.IrSetVariable.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSpreadElement(element: IrSpreadElement): KonanIr.IrSpreadElement {
        val coordinates = serializeCoordinates(element.startOffset, element.endOffset)
        return KonanIr.IrSpreadElement.newBuilder()
            .setExpression(serializeExpression(element.expression))
            .setCoordinates(coordinates)
            .build()
    }

    private fun serializeSyntheticBody(expression: IrSyntheticBody) = KonanIr.IrSyntheticBody.newBuilder()
        .setKind(
            when (expression.kind) {
                IrSyntheticBodyKind.ENUM_VALUES -> KonanIr.IrSyntheticBodyKind.ENUM_VALUES
                IrSyntheticBodyKind.ENUM_VALUEOF -> KonanIr.IrSyntheticBodyKind.ENUM_VALUEOF
            }
        )
        .build()

    private fun serializeThrow(expression: IrThrow): KonanIr.IrThrow {
        val proto = KonanIr.IrThrow.newBuilder()
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeTry(expression: IrTry): KonanIr.IrTry {
        val proto = KonanIr.IrTry.newBuilder()
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

    private fun serializeTypeOperator(operator: IrTypeOperator): KonanIr.IrTypeOperator = when (operator) {
        IrTypeOperator.CAST
        -> KonanIr.IrTypeOperator.CAST
        IrTypeOperator.IMPLICIT_CAST
        -> KonanIr.IrTypeOperator.IMPLICIT_CAST
        IrTypeOperator.IMPLICIT_NOTNULL
        -> KonanIr.IrTypeOperator.IMPLICIT_NOTNULL
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        -> KonanIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        IrTypeOperator.IMPLICIT_INTEGER_COERCION
        -> KonanIr.IrTypeOperator.IMPLICIT_INTEGER_COERCION
        IrTypeOperator.SAFE_CAST
        -> KonanIr.IrTypeOperator.SAFE_CAST
        IrTypeOperator.INSTANCEOF
        -> KonanIr.IrTypeOperator.INSTANCEOF
        IrTypeOperator.NOT_INSTANCEOF
        -> KonanIr.IrTypeOperator.NOT_INSTANCEOF
        IrTypeOperator.SAM_CONVERSION
        -> KonanIr.IrTypeOperator.SAM_CONVERSION
    }

    private fun serializeTypeOp(expression: IrTypeOperatorCall): KonanIr.IrTypeOp {
        val proto = KonanIr.IrTypeOp.newBuilder()
            .setOperator(serializeTypeOperator(expression.operator))
            .setOperand(serializeIrType(expression.typeOperand))
            .setArgument(serializeExpression(expression.argument))
        return proto.build()

    }

    private fun serializeVararg(expression: IrVararg): KonanIr.IrVararg {
        val proto = KonanIr.IrVararg.newBuilder()
            .setElementType(serializeIrType(expression.varargElementType))
        expression.elements.forEach {
            proto.addElement(serializeVarargElement(it))
        }
        return proto.build()
    }

    private fun serializeVarargElement(element: IrVarargElement): KonanIr.IrVarargElement {
        val proto = KonanIr.IrVarargElement.newBuilder()
        when (element) {
            is IrExpression
            -> proto.expression = serializeExpression(element)
            is IrSpreadElement
            -> proto.spreadElement = serializeSpreadElement(element)
            else -> error("Unknown vararg element kind")
        }
        return proto.build()
    }

    private fun serializeWhen(expression: IrWhen): KonanIr.IrWhen {
        val proto = KonanIr.IrWhen.newBuilder()

        val branches = expression.branches
        branches.forEach {
            proto.addBranch(serializeStatement(it))
        }

        return proto.build()
    }

    private fun serializeLoop(expression: IrLoop): KonanIr.Loop {
        val proto = KonanIr.Loop.newBuilder()
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

    private fun serializeWhile(expression: IrWhileLoop): KonanIr.IrWhile {
        val proto = KonanIr.IrWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    private fun serializeBreak(expression: IrBreak): KonanIr.IrBreak {
        val proto = KonanIr.IrBreak.newBuilder()
        val label = expression.label?.let { serializeString(it) }
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeContinue(expression: IrContinue): KonanIr.IrContinue {
        val proto = KonanIr.IrContinue.newBuilder()
        val label = expression.label?.let { serializeString(it) }
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeExpression(expression: IrExpression): KonanIr.IrExpression {
        logger.log { "### serializing Expression: ${ir2string(expression)}" }

        val coordinates = serializeCoordinates(expression.startOffset, expression.endOffset)
        val proto = KonanIr.IrExpression.newBuilder()
            .setType(serializeIrType(expression.type))
            .setCoordinates(coordinates)

        val operationProto = KonanIr.IrOperation.newBuilder()

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
            else -> {
                TODO("Expression serialization not implemented yet: ${ir2string(expression)}.")
            }
        }
        proto.setOperation(operationProto)

        return proto.build()
    }

    private fun serializeStatement(statement: IrElement): KonanIr.IrStatement {
        logger.log { "### serializing Statement: ${ir2string(statement)}" }

        val coordinates = serializeCoordinates(statement.startOffset, statement.endOffset)
        val proto = KonanIr.IrStatement.newBuilder()
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

    private fun serializeIrTypeAlias(typeAlias: IrTypeAlias) = KonanIr.IrTypeAlias.newBuilder().build()

    private fun serializeIrValueParameter(parameter: IrValueParameter): KonanIr.IrValueParameter {
        val proto = KonanIr.IrValueParameter.newBuilder()
            .setSymbol(serializeIrSymbol(parameter.symbol))
            .setName(serializeString(parameter.name.toString()))
            .setIndex(parameter.index)
            .setType(serializeIrType(parameter.type))
            .setIsCrossinline(parameter.isCrossinline)
            .setIsNoinline(parameter.isNoinline)

        parameter.varargElementType?.let { proto.setVarargElementType(serializeIrType(it)) }
        parameter.defaultValue?.let { proto.setDefaultValue(serializeExpression(it.expression)) }

        return proto.build()
    }

    private fun serializeIrTypeParameter(parameter: IrTypeParameter): KonanIr.IrTypeParameter {
        val proto = KonanIr.IrTypeParameter.newBuilder()
            .setSymbol(serializeIrSymbol(parameter.symbol))
            .setName(serializeString(parameter.name.toString()))
            .setIndex(parameter.index)
            .setVariance(serializeIrTypeVariance(parameter.variance))
            .setIsReified(parameter.isReified)
        parameter.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }
        return proto.build()
    }

    private fun serializeIrTypeParameterContainer(typeParameters: List<IrTypeParameter>): KonanIr.IrTypeParameterContainer {
        val proto = KonanIr.IrTypeParameterContainer.newBuilder()
        typeParameters.forEach {
            proto.addTypeParameter(serializeDeclaration(it))
        }
        return proto.build()
    }

    private fun serializeIrFunctionBase(function: IrFunction): KonanIr.IrFunctionBase {
        val proto = KonanIr.IrFunctionBase.newBuilder()
            .setName(serializeString(function.name.toString()))
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
        Modality.FINAL -> KonanIr.ModalityKind.FINAL_MODALITY
        Modality.SEALED -> KonanIr.ModalityKind.SEALED_MODALITY
        Modality.OPEN -> KonanIr.ModalityKind.OPEN_MODALITY
        Modality.ABSTRACT -> KonanIr.ModalityKind.ABSTRACT_MODALITY
    }

    private fun serializeIrConstructor(declaration: IrConstructor): KonanIr.IrConstructor =
        KonanIr.IrConstructor.newBuilder()
            .setSymbol(serializeIrSymbol(declaration.symbol))
            .setBase(serializeIrFunctionBase(declaration))
            .setIsPrimary(declaration.isPrimary)
            .build()

    private fun serializeIrFunction(declaration: IrSimpleFunction): KonanIr.IrFunction {
        val function = declaration// as IrFunctionImpl
        val proto = KonanIr.IrFunction.newBuilder()
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

    private fun serializeIrAnonymousInit(declaration: IrAnonymousInitializer) = KonanIr.IrAnonymousInit.newBuilder()
        .setSymbol(serializeIrSymbol(declaration.symbol))
        .setBody(serializeStatement(declaration.body))
        .build()

    private fun serializeIrProperty(property: IrProperty): KonanIr.IrProperty {
        val index = declarationTable.uniqIdByDeclaration(property)

        val proto = KonanIr.IrProperty.newBuilder()
            .setIsDelegated(property.isDelegated)
            .setName(serializeString(property.name.toString()))
            .setVisibility(serializeVisibility(property.visibility))
            .setModality(serializeModality(property.modality))
            .setIsVar(property.isVar)
            .setIsConst(property.isConst)
            .setIsLateinit(property.isLateinit)
            .setIsDelegated(property.isDelegated)
            .setIsExternal(property.isExternal)

        descriptorReferenceSerializer.serializeDescriptorReference(property)?.let { proto.setDescriptor(it) }

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

    private fun serializeIrField(field: IrField): KonanIr.IrField {
        val proto = KonanIr.IrField.newBuilder()
            .setSymbol(serializeIrSymbol(field.symbol))
            .setName(serializeString(field.name.toString()))
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

    private fun serializeIrVariable(variable: IrVariable): KonanIr.IrVariable {
        val proto = KonanIr.IrVariable.newBuilder()
            .setSymbol(serializeIrSymbol(variable.symbol))
            .setName(serializeString(variable.name.toString()))
            .setType(serializeIrType(variable.type))
            .setIsConst(variable.isConst)
            .setIsVar(variable.isVar)
            .setIsLateinit(variable.isLateinit)
        variable.initializer?.let { proto.initializer = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeIrDeclarationContainer(declarations: List<IrDeclaration>): KonanIr.IrDeclarationContainer {
        val proto = KonanIr.IrDeclarationContainer.newBuilder()
        declarations.forEach {
            //if (it is IrDeclarationWithVisibility && it.visibility == Visibilities.INVISIBLE_FAKE) return@forEach
            proto.addDeclaration(serializeDeclaration(it))
        }
        return proto.build()
    }

    private fun serializeClassKind(kind: ClassKind) = when (kind) {
        CLASS -> KonanIr.ClassKind.CLASS
        INTERFACE -> KonanIr.ClassKind.INTERFACE
        ENUM_CLASS -> KonanIr.ClassKind.ENUM_CLASS
        ENUM_ENTRY -> KonanIr.ClassKind.ENUM_ENTRY
        ANNOTATION_CLASS -> KonanIr.ClassKind.ANNOTATION_CLASS
        OBJECT -> KonanIr.ClassKind.OBJECT
    }

    private fun serializeIrClass(clazz: IrClass): KonanIr.IrClass {
        val proto = KonanIr.IrClass.newBuilder()
            .setName(serializeString(clazz.name.toString()))
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

    private fun serializeIrEnumEntry(enumEntry: IrEnumEntry): KonanIr.IrEnumEntry {
        val proto = KonanIr.IrEnumEntry.newBuilder()
            .setName(serializeString(enumEntry.name.toString()))
            .setSymbol(serializeIrSymbol(enumEntry.symbol))

        enumEntry.initializerExpression?.let {
            proto.initializer = serializeExpression(it)
        }
        enumEntry.correspondingClass?.let {
            proto.correspondingClass = serializeDeclaration(it)
        }
        return proto.build()
    }

    private fun serializeDeclaration(declaration: IrDeclaration): KonanIr.IrDeclaration {
        logger.log { "### serializing Declaration: ${ir2string(declaration)}" }

        val declarator = KonanIr.IrDeclarator.newBuilder()

        when (declaration) {
            is IrTypeAlias
            -> declarator.irTypeAlias = serializeIrTypeAlias(declaration)
            is IrAnonymousInitializer
            -> declarator.irAnonymousInit = serializeIrAnonymousInit(declaration)
            is IrConstructor
            -> declarator.irConstructor = serializeIrConstructor(declaration)
            is IrField
            -> declarator.irField = serializeIrField(declaration)
            is IrSimpleFunction
            -> declarator.irFunction = serializeIrFunction(declaration)
            is IrTypeParameter
            -> declarator.irTypeParameter = serializeIrTypeParameter(declaration)
            is IrVariable
            -> declarator.irVariable = serializeIrVariable(declaration)
            is IrValueParameter
            -> declarator.irValueParameter = serializeIrValueParameter(declaration)
            is IrClass
            -> declarator.irClass = serializeIrClass(declaration)
            is IrEnumEntry
            -> declarator.irEnumEntry = serializeIrEnumEntry(declaration)
            is IrProperty
            -> declarator.irProperty = serializeIrProperty(declaration)
            else
            -> TODO("Declaration serialization not supported yet: $declaration")
        }

        val coordinates = serializeCoordinates(declaration.startOffset, declaration.endOffset)
        val annotations = serializeAnnotations(declaration.annotations)
        val origin = serializeIrDeclarationOrigin(declaration.origin)
        val proto = KonanIr.IrDeclaration.newBuilder()
            .setCoordinates(coordinates)
            .setAnnotations(annotations)
            .setOrigin(origin)


        proto.setDeclarator(declarator)

        return proto.build()
    }

// ---------- Top level ------------------------------------------------------

    fun serializeFileEntry(entry: SourceManager.FileEntry) = KonanIr.FileEntry.newBuilder()
        .setName(serializeString(entry.name))
        .addAllLineStartOffsets(entry.lineStartOffsets.asIterable())
        .build()

    fun serializeIrFile(file: IrFile): KonanIr.IrFile {
        val proto = KonanIr.IrFile.newBuilder()
            .setFileEntry(serializeFileEntry(file.fileEntry))
            .setFqName(serializeString(file.fqName.toString()))
            .setAnnotations(serializeAnnotations(file.annotations))

        file.declarations.forEach {
            if (it is IrTypeAlias || (it.descriptor.isExpectMember && !it.descriptor.isSerializableExpectClass)) {
                writer.skipDeclaration()
                return@forEach
            }

            val byteArray = serializeDeclaration(it).toByteArray()
            val uniqId = declarationTable.uniqIdByDeclaration(it)
            writer.addDeclaration(DeclarationId(uniqId.index, uniqId.isLocal), byteArray)
            proto.addDeclarationId(protoUniqId(uniqId))
        }

        file.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                if (declaration.descriptor.annotations.hasAnnotation(RuntimeNames.exportForCppRuntime)
                        || declaration.descriptor.annotations.hasAnnotation(RuntimeNames.exportForCompilerAnnotation))
                    proto.addExplicitlyExportedToCompiler(serializeIrSymbol(declaration.symbol))
                super.visitDeclaration(declaration)
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration.descriptor.annotations.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation))
                    proto.addExplicitlyExportedToCompiler(serializeIrSymbol(declaration.symbol))
                super.visitDeclaration(declaration)
            }
        })

        return proto.build()
    }

    lateinit var writer: CombinedIrFileWriter

    fun serializeModule(module: IrModuleFragment): KonanIr.IrModule {
        val proto = KonanIr.IrModule.newBuilder()
            .setName(serializeString(module.name.toString()))

        val topLevelDeclarationsCount = module.files.sumBy { it.declarations.size }

        writer = CombinedIrFileWriter(topLevelDeclarationsCount)

        module.files.forEach {
            proto.addFile(serializeIrFile(it))
        }
        proto.symbolTable = KonanIr.IrSymbolTable.newBuilder()
            .addAllSymbols(protoSymbolArray)
            .build()

        proto.typeTable = KonanIr.IrTypeTable.newBuilder()
            .addAllTypes(protoTypeArray)
            .build()

        proto.stringTable = KonanIr.StringTable.newBuilder()
            .addAllStrings(protoStringArray)
            .build()

        return proto.build()
    }


    fun serializedIrModule(module: IrModuleFragment): SerializedIr {
        val moduleHeader = serializeModule(module).toByteArray()
        return SerializedIr(moduleHeader, writer.finishWriting().absolutePath, declarationTable.debugIndex)
    }
}
