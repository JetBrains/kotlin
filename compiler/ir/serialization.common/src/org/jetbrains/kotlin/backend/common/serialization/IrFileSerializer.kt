/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.findTopLevelDeclaration
import org.jetbrains.kotlin.ir.util.lineStartOffsets
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.SerializedDeclaration
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SkippedDeclaration
import org.jetbrains.kotlin.library.TopLevelDeclaration
import org.jetbrains.kotlin.library.impl.IrMemoryArrayWriter
import org.jetbrains.kotlin.library.impl.IrMemoryDeclarationWriter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.backend.common.serialization.proto.ClassKind as ProtoClassKind
import org.jetbrains.kotlin.backend.common.serialization.proto.Coordinates as ProtoCoordinates
import org.jetbrains.kotlin.backend.common.serialization.proto.FieldAccessCommon as ProtoFieldAccessCommon
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrAnonymousInit as ProtoAnonymousInit
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBlock as ProtoBlock
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBlockBody as ProtoBlockBody
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBranch as ProtoBranch
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBreak as ProtoBreak
import org.jetbrains.kotlin.backend.common.serialization.proto.IrCall as ProtoCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrCatch as ProtoCatch
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClassReference as ProtoClassReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrComposite as ProtoComposite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConst as ProtoConst
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructor as ProtoConstructor
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrContinue as ProtoContinue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase as ProtoDeclarationBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer as ProtoDeclarationContainer
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationOrigin as ProtoDeclarationOrigin
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDelegatingConstructorCall as ProtoDelegatingConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDoWhile as ProtoDoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicMemberExpression as ProtoDynamicMemberExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicOperatorExpression as ProtoDynamicOperatorExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicType as ProtoDynamicType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumConstructorCall as ProtoEnumConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumEntry as ProtoEnumEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorType as ProtoErrorType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrField as ProtoField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionExpression as ProtoFunctionExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionReference as ProtoFunctionReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetClass as ProtoGetClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetEnumValue as ProtoGetEnumValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetField as ProtoGetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetObject as ProtoGetObject
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetValue as ProtoGetValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInstanceInitializerCall as ProtoInstanceInitializerCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedProperty as ProtoLocalDelegatedProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedPropertyReference as ProtoLocalDelegatedPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrOperation as ProtoOperation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrPropertyReference as ProtoPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturn as ProtoReturn
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetField as ProtoSetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetVariable as ProtoSetVariable
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleType as ProtoSimpleType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSpreadElement as ProtoSpreadElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStarProjection as ProtoStarProjection
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatementOrigin as ProtoStatementOrigin
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStringConcat as ProtoStringConcat
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSymbolData as ProtoSymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSymbolKind as ProtoSymbolKind
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBody as ProtoSyntheticBody
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBodyKind as ProtoSyntheticBodyKind
import org.jetbrains.kotlin.backend.common.serialization.proto.IrThrow as ProtoThrow
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTry as ProtoTry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAbbreviation as ProtoTypeAbbreviation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAlias as ProtoTypeAlias
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeArgument as ProtoTypeArgument
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOp as ProtoTypeOp
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOperator as ProtoTypeOperator
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter as ProtoTypeParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameterContainer as ProtoTypeParameterContainer
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeProjection as ProtoTypeProjection
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance as ProtoTypeVariance
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoValueParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVararg as ProtoVararg
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVarargElement as ProtoVarargElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVariable as ProtoVariable
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhen as ProtoWhen
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhile as ProtoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.Loop as ProtoLoop
import org.jetbrains.kotlin.backend.common.serialization.proto.MemberAccessCommon as ProtoMemberAccessCommon
import org.jetbrains.kotlin.backend.common.serialization.proto.ModalityKind as ProtoModalityKind
import org.jetbrains.kotlin.backend.common.serialization.proto.NullableIrExpression as ProtoNullableIrExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.TypeArguments as ProtoTypeArguments
import org.jetbrains.kotlin.backend.common.serialization.proto.Visibility as ProtoVisibility

open class IrFileSerializer(
    val logger: LoggingContext,
    private val declarationTable: DeclarationTable,
    private val bodiesOnlyForInlines: Boolean = false
) {

    private val loopIndex = mutableMapOf<IrLoop, Int>()
    private var currentLoopIndex = 0

    // The same symbol can be used multiple times in a file
    // so use this index to store symbol data only once.
    private val protoSymbolMap = mutableMapOf<IrSymbol, Int>()
    private val protoSymbolArray = arrayListOf<ProtoSymbolData>()

    // The same type can be used multiple times in a file
    // so use this index to store type data only once.
    private val protoTypeMap = mutableMapOf<IrTypeKey, Int>()
    private val protoTypeArray = arrayListOf<ProtoType>()

    private val protoStringMap = mutableMapOf<String, Int>()
    private val protoStringArray = arrayListOf<String>()

    private val protoBodyArray = mutableListOf<XStatementOrExpression>()

    private val descriptorReferenceSerializer =
        DescriptorReferenceSerializer(declarationTable, { serializeString(it) }, { serializeFqName(it) })

    sealed class XStatementOrExpression {
        abstract fun toByteArray(): ByteArray

        open fun toProtoStatement(): ProtoStatement {
            error("It is not a ProtoStatement")
        }

        open fun toProtoExpression(): ProtoExpression {
            error("It is not a ProtoExpression")
        }

        class XStatement(private val proto: ProtoStatement) : XStatementOrExpression() {
            override fun toByteArray(): ByteArray = proto.toByteArray()
            override fun toProtoStatement() = proto
        }

        class XExpression(private val proto: ProtoExpression) : XStatementOrExpression() {
            override fun toByteArray(): ByteArray = proto.toByteArray()
            override fun toProtoExpression() = proto
        }
    }

    private fun serializeIrExpressionBody(expression: IrExpression): Int {
        protoBodyArray.add(XStatementOrExpression.XExpression(serializeExpression(expression)))
        return protoBodyArray.size - 1
    }

    private fun serializeIrStatementBody(statement: IrElement): Int {
        protoBodyArray.add(XStatementOrExpression.XStatement(serializeStatement(statement)))
        return protoBodyArray.size - 1
    }

    /* ------- Common fields ---------------------------------------------------- */

    private fun serializeIrDeclarationOrigin(origin: IrDeclarationOrigin) =
        ProtoDeclarationOrigin.newBuilder()
            .setCustom(serializeString((origin as IrDeclarationOriginImpl).name))
            .build()

    private fun serializeIrStatementOrigin(origin: IrStatementOrigin) =
        ProtoStatementOrigin.newBuilder()
            .setName(serializeString((origin as IrStatementOriginImpl).debugName))
            .build()

    private fun serializeVisibility(visibility: Visibility) =
        ProtoVisibility.newBuilder()
            .setName(serializeString(visibility.name))
            .build()

    private fun serializeCoordinates(start: Int, end: Int): ProtoCoordinates {
        return ProtoCoordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build()
    }

    /* ------- Strings ---------------------------------------------------------- */

    private fun serializeString(value: String): Int = protoStringMap.getOrPut(value) {
        protoStringArray.add(value)
        protoStringArray.size - 1
    }

    private fun serializeName(name: Name): Int = serializeString(name.toString())

    /* ------- IrSymbols -------------------------------------------------------- */

    private fun protoSymbolKind(symbol: IrSymbol): ProtoSymbolKind = when (symbol) {
        is IrAnonymousInitializerSymbol ->
            ProtoSymbolKind.ANONYMOUS_INIT_SYMBOL
        is IrClassSymbol ->
            ProtoSymbolKind.CLASS_SYMBOL
        is IrConstructorSymbol ->
            ProtoSymbolKind.CONSTRUCTOR_SYMBOL
        is IrTypeParameterSymbol ->
            ProtoSymbolKind.TYPE_PARAMETER_SYMBOL
        is IrEnumEntrySymbol ->
            ProtoSymbolKind.ENUM_ENTRY_SYMBOL
        is IrVariableSymbol ->
            ProtoSymbolKind.VARIABLE_SYMBOL
        is IrValueParameterSymbol ->
            if (symbol.descriptor is ReceiverParameterDescriptor) // TODO: we use descriptor here.
                ProtoSymbolKind.RECEIVER_PARAMETER_SYMBOL
            else
                ProtoSymbolKind.VALUE_PARAMETER_SYMBOL
        is IrSimpleFunctionSymbol ->
            ProtoSymbolKind.FUNCTION_SYMBOL
        is IrReturnableBlockSymbol ->
            ProtoSymbolKind.RETURNABLE_BLOCK_SYMBOL
        is IrFieldSymbol ->
            if (symbol.owner.correspondingPropertySymbol?.owner.let { it == null || it.isDelegated })
                ProtoSymbolKind.STANDALONE_FIELD_SYMBOL
            else
                ProtoSymbolKind.FIELD_SYMBOL
        is IrPropertySymbol ->
            ProtoSymbolKind.PROPERTY_SYMBOL
        is IrLocalDelegatedPropertySymbol ->
            ProtoSymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL
        is IrTypeAliasSymbol ->
            ProtoSymbolKind.TYPEALIAS_SYMBOL
        else ->
            TODO("Unexpected symbol kind: $symbol")
    }

    private fun serializeIrSymbolData(symbol: IrSymbol): ProtoSymbolData {

        val declaration = symbol.owner as? IrDeclaration ?: error("Expected IrDeclaration: ${symbol.owner}")

        val proto = ProtoSymbolData.newBuilder()
        proto.kind = protoSymbolKind(symbol)

        val uniqId =
            declarationTable.uniqIdByDeclaration(declaration)
        proto.uniqIdIndex = uniqId.index

        val topLevelUniqId =
            declarationTable.uniqIdByDeclaration((declaration).findTopLevelDeclaration())

        proto.topLevelUniqIdIndex = topLevelUniqId.index

        descriptorReferenceSerializer.serializeDescriptorReference(declaration)?.let {
            proto.setDescriptorReference(it)
        }

        return proto.build()
    }

    fun serializeIrSymbol(symbol: IrSymbol) = protoSymbolMap.getOrPut(symbol) {
        val symbolData = serializeIrSymbolData(symbol)
        protoSymbolArray.add(symbolData)
        protoSymbolArray.size - 1
    }

    /* ------- IrTypes ---------------------------------------------------------- */

    // TODO: we, probably, need a type table.

    private fun serializeTypeArguments(call: IrMemberAccessExpression): ProtoTypeArguments {
        val proto = ProtoTypeArguments.newBuilder()
        for (i in 0 until call.typeArgumentsCount) {
            proto.addTypeArgument(serializeIrType(call.getTypeArgument(i)!!))
        }
        return proto.build()
    }

    private fun serializeIrTypeVariance(variance: Variance) = when (variance) {
        Variance.IN_VARIANCE -> ProtoTypeVariance.IN
        Variance.OUT_VARIANCE -> ProtoTypeVariance.OUT
        Variance.INVARIANT -> ProtoTypeVariance.INV
    }

    private fun serializeAnnotations(annotations: List<IrConstructorCall>) =
        annotations.map { serializeConstructorCall(it) }

    private fun serializeFqName(fqName: FqName) = fqName.pathSegments().map { serializeString(it.identifier) }

    private fun serializeIrTypeProjection(argument: IrTypeProjection): ProtoTypeProjection = ProtoTypeProjection.newBuilder()
        .setVariance(serializeIrTypeVariance(argument.variance))
        .setType(serializeIrType(argument.type))
        .build()

    private fun serializeTypeArgument(argument: IrTypeArgument): ProtoTypeArgument {
        val proto = ProtoTypeArgument.newBuilder()
        when (argument) {
            is IrStarProjection ->
                proto.star = ProtoStarProjection.newBuilder()
                    .build() // TODO: Do we need a singletone here? Or just an enum?
            is IrTypeProjection ->
                proto.type = serializeIrTypeProjection(argument)
            else -> TODO("Unexpected type argument kind: $argument")
        }
        return proto.build()
    }

    private fun serializeSimpleType(type: IrSimpleType): ProtoSimpleType {
        val proto = ProtoSimpleType.newBuilder()
            .addAllAnnotation(serializeAnnotations(type.annotations))
            .setClassifier(serializeIrSymbol(type.classifier))
            .setHasQuestionMark(type.hasQuestionMark)
        type.abbreviation?.let {
            proto.setAbbreviation(serializeIrTypeAbbreviation(it))
        }
        type.arguments.forEach {
            proto.addArgument(serializeTypeArgument(it))
        }
        return proto.build()
    }

    private fun serializeIrTypeAbbreviation(typeAbbreviation: IrTypeAbbreviation): ProtoTypeAbbreviation {
        val proto = ProtoTypeAbbreviation.newBuilder()
            .addAllAnnotation(serializeAnnotations(typeAbbreviation.annotations))
            .setTypeAlias(serializeIrSymbol(typeAbbreviation.typeAlias))
            .setHasQuestionMark(typeAbbreviation.hasQuestionMark)
        typeAbbreviation.arguments.forEach {
            proto.addArgument(serializeTypeArgument(it))
        }
        return proto.build()
    }

    private fun serializeDynamicType(type: IrDynamicType): ProtoDynamicType = ProtoDynamicType.newBuilder()
        .addAllAnnotation(serializeAnnotations(type.annotations))
        .build()

    private fun serializeErrorType(type: IrErrorType): ProtoErrorType = ProtoErrorType.newBuilder()
        .addAllAnnotation(serializeAnnotations(type.annotations))
        .build()

    private fun serializeIrTypeData(type: IrType): ProtoType {
        logger.log { "### serializing IrType: $type" }
        val proto = ProtoType.newBuilder()
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
    data class IrTypeKey(
        val kind: IrTypeKind,
        val classifier: IrClassifierSymbol?,
        val hasQuestionMark: Boolean?,
        val arguments: List<IrTypeArgumentKey>?,
        val annotations: List<IrConstructorCall>
    )

    data class IrTypeArgumentKey(
        val kind: IrTypeArgumentKind,
        val variance: Variance?,
        val type: IrTypeKey?
    )

    private val IrType.toIrTypeKey: IrTypeKey
        get() = IrTypeKey(
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

    private val IrTypeArgument.toIrTypeArgumentKey: IrTypeArgumentKey
        get() = IrTypeArgumentKey(
            kind = when (this) {
                is IrStarProjection -> IrTypeArgumentKind.STAR
                is IrTypeProjection -> IrTypeArgumentKind.PROJECTION
                else -> error("Unexpected type argument kind: $this")
            },
            variance = (this as? IrTypeProjection)?.variance,
            type = (this as? IrTypeProjection)?.type?.toIrTypeKey
        )

    private fun serializeIrType(type: IrType) = protoTypeMap.getOrPut(type.toIrTypeKey) {
        protoTypeArray.add(serializeIrTypeData(type))
        protoTypeArray.size - 1
    }

    /* -------------------------------------------------------------------------- */

    private fun serializeBlockBody(expression: IrBlockBody): ProtoBlockBody {
        val proto = ProtoBlockBody.newBuilder()
        expression.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeBranch(branch: IrBranch): ProtoBranch {
        val proto = ProtoBranch.newBuilder()

        proto.condition = serializeExpression(branch.condition)
        proto.result = serializeExpression(branch.result)

        return proto.build()
    }

    private fun serializeBlock(block: IrBlock): ProtoBlock {
        val proto = ProtoBlock.newBuilder()

        block.origin?.let { proto.setOrigin(serializeIrStatementOrigin(it)) }

        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeComposite(composite: IrComposite): ProtoComposite {
        val proto = ProtoComposite.newBuilder()

        composite.origin?.let { proto.setOrigin(serializeIrStatementOrigin(it)) }
        composite.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeCatch(catch: IrCatch): ProtoCatch {
        val proto = ProtoCatch.newBuilder()
            .setCatchParameter(serializeIrVariable(catch.catchParameter))
            .setResult(serializeExpression(catch.result))
        return proto.build()
    }

    private fun serializeStringConcat(expression: IrStringConcatenation): ProtoStringConcat {
        val proto = ProtoStringConcat.newBuilder()
        expression.arguments.forEach {
            proto.addArgument(serializeExpression(it))
        }
        return proto.build()
    }

    private fun serializeMemberAccessCommon(call: IrMemberAccessExpression): ProtoMemberAccessCommon {
        val proto = ProtoMemberAccessCommon.newBuilder()
        if (call.extensionReceiver != null) {
            proto.extensionReceiver = serializeExpression(call.extensionReceiver!!)
        }

        if (call.dispatchReceiver != null) {
            proto.dispatchReceiver = serializeExpression(call.dispatchReceiver!!)
        }
        proto.typeArguments = serializeTypeArguments(call)

        for (index in 0 until call.valueArgumentsCount) {
            val actual = call.getValueArgument(index)
            val argOrNull = ProtoNullableIrExpression.newBuilder()
            if (actual == null) {
                // Am I observing an IR generation regression?
                // I see a lack of arg for an empty vararg,
                // rather than an empty vararg node.

                // TODO: how do we assert that without descriptor?
                //assert(it.varargElementType != null || it.hasDefaultValue())
            } else {
                argOrNull.expression = serializeExpression(actual)
            }
            proto.addValueArgument(argOrNull)
        }
        return proto.build()
    }

    private fun serializeCall(call: IrCall): ProtoCall {
        val proto = ProtoCall.newBuilder()
        proto.symbol = serializeIrSymbol(call.symbol)
        call.origin?.let { proto.origin = serializeIrStatementOrigin(it) }

        call.superQualifierSymbol?.let {
            proto.`super` = serializeIrSymbol(it)
        }
        proto.memberAccess = serializeMemberAccessCommon(call)

        return proto.build()
    }

    private fun serializeConstructorCall(call: IrConstructorCall): ProtoConstructorCall =
        ProtoConstructorCall.newBuilder().apply {
            symbol = serializeIrSymbol(call.symbol)
            constructorTypeArgumentsCount = call.constructorTypeArgumentsCount
            memberAccess = serializeMemberAccessCommon(call)
        }.build()

    private fun serializeFunctionExpression(functionExpression: IrFunctionExpression): ProtoFunctionExpression =
        ProtoFunctionExpression.newBuilder().apply {
            function = serializeIrFunction(functionExpression.function)
            origin = serializeIrStatementOrigin(functionExpression.origin)
        }.build()

    private fun serializeFunctionReference(callable: IrFunctionReference): ProtoFunctionReference {
        val proto = ProtoFunctionReference.newBuilder()
            .setSymbol(serializeIrSymbol(callable.symbol))
            .setMemberAccess(serializeMemberAccessCommon(callable))

        callable.origin?.let { proto.setOrigin(serializeIrStatementOrigin(it)) }
        return proto.build()
    }

    private fun serializeIrLocalDelegatedPropertyReference(
        callable: IrLocalDelegatedPropertyReference
    ): ProtoLocalDelegatedPropertyReference {
        val proto = ProtoLocalDelegatedPropertyReference.newBuilder()
            .setDelegate(serializeIrSymbol(callable.delegate))
            .setGetter(serializeIrSymbol(callable.getter))
            .setSymbol(serializeIrSymbol(callable.symbol))

        callable.origin?.let { proto.setOrigin(serializeIrStatementOrigin(it)) }
        callable.setter?.let { proto.setSetter(serializeIrSymbol(it)) }

        return proto.build()
    }

    private fun serializePropertyReference(callable: IrPropertyReference): ProtoPropertyReference {
        val proto = ProtoPropertyReference.newBuilder()
            .setMemberAccess(serializeMemberAccessCommon(callable))
            .setSymbol(serializeIrSymbol(callable.symbol))
        callable.origin?.let { proto.origin = serializeIrStatementOrigin(it) }
        callable.field?.let { proto.field = serializeIrSymbol(it) }
        callable.getter?.let { proto.getter = serializeIrSymbol(it) }
        callable.setter?.let { proto.setter = serializeIrSymbol(it) }

        return proto.build()
    }

    private fun serializeClassReference(expression: IrClassReference): ProtoClassReference {
        val proto = ProtoClassReference.newBuilder()
            .setClassSymbol(serializeIrSymbol(expression.symbol))
            .setClassType(serializeIrType(expression.classType))
        return proto.build()
    }

    private fun serializeConst(value: IrConst<*>): ProtoConst {
        val proto = ProtoConst.newBuilder()
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

    private fun serializeDelegatingConstructorCall(call: IrDelegatingConstructorCall): ProtoDelegatingConstructorCall {
        val proto = ProtoDelegatingConstructorCall.newBuilder()
            .setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeDoWhile(expression: IrDoWhileLoop): ProtoDoWhile =
        ProtoDoWhile.newBuilder()
            .setLoop(serializeLoop(expression))
            .build()

    private fun serializeEnumConstructorCall(call: IrEnumConstructorCall): ProtoEnumConstructorCall {
        val proto = ProtoEnumConstructorCall.newBuilder()
            .setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeGetClass(expression: IrGetClass): ProtoGetClass {
        val proto = ProtoGetClass.newBuilder()
            .setArgument(serializeExpression(expression.argument))
        return proto.build()
    }

    private fun serializeGetEnumValue(expression: IrGetEnumValue): ProtoGetEnumValue {
        val proto = ProtoGetEnumValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeFieldAccessCommon(expression: IrFieldAccessExpression): ProtoFieldAccessCommon {
        val proto = ProtoFieldAccessCommon.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        expression.superQualifierSymbol?.let { proto.`super` = serializeIrSymbol(it) }
        expression.receiver?.let { proto.receiver = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeGetField(expression: IrGetField): ProtoGetField =
        ProtoGetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression)).apply {
                expression.origin?.let { origin = serializeIrStatementOrigin(it) }
            }
            .build()

    private fun serializeGetValue(expression: IrGetValue): ProtoGetValue =
        ProtoGetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol)).apply {
                expression.origin?.let { origin = serializeIrStatementOrigin(it) }
            }
            .build()

    private fun serializeGetObject(expression: IrGetObjectValue): ProtoGetObject {
        val proto = ProtoGetObject.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeInstanceInitializerCall(call: IrInstanceInitializerCall): ProtoInstanceInitializerCall {
        val proto = ProtoInstanceInitializerCall.newBuilder()

        proto.symbol = serializeIrSymbol(call.classSymbol)

        return proto.build()
    }

    private fun serializeReturn(expression: IrReturn): ProtoReturn {
        val proto = ProtoReturn.newBuilder()
            .setReturnTarget(serializeIrSymbol(expression.returnTargetSymbol))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetField(expression: IrSetField): ProtoSetField =
        ProtoSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value)).apply {
                expression.origin?.let { origin = serializeIrStatementOrigin(it) }
            }
            .build()

    private fun serializeSetVariable(expression: IrSetVariable): ProtoSetVariable =
        ProtoSetVariable.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
            .setValue(serializeExpression(expression.value)).apply {
                expression.origin?.let { origin = serializeIrStatementOrigin(it) }
            }
            .build()

    private fun serializeSpreadElement(element: IrSpreadElement): ProtoSpreadElement {
        val coordinates = serializeCoordinates(element.startOffset, element.endOffset)
        return ProtoSpreadElement.newBuilder()
            .setExpression(serializeExpression(element.expression))
            .setCoordinates(coordinates)
            .build()
    }

    private fun serializeSyntheticBody(expression: IrSyntheticBody) = ProtoSyntheticBody.newBuilder()
        .setKind(
            when (expression.kind) {
                IrSyntheticBodyKind.ENUM_VALUES -> ProtoSyntheticBodyKind.ENUM_VALUES
                IrSyntheticBodyKind.ENUM_VALUEOF -> ProtoSyntheticBodyKind.ENUM_VALUEOF
            }
        )
        .build()

    private fun serializeThrow(expression: IrThrow): ProtoThrow {
        val proto = ProtoThrow.newBuilder()
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeTry(expression: IrTry): ProtoTry {
        val proto = ProtoTry.newBuilder()
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

    private fun serializeTypeOperator(operator: IrTypeOperator): ProtoTypeOperator = when (operator) {
        IrTypeOperator.CAST ->
            ProtoTypeOperator.CAST
        IrTypeOperator.IMPLICIT_CAST ->
            ProtoTypeOperator.IMPLICIT_CAST
        IrTypeOperator.IMPLICIT_NOTNULL ->
            ProtoTypeOperator.IMPLICIT_NOTNULL
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
            ProtoTypeOperator.IMPLICIT_COERCION_TO_UNIT
        IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
            ProtoTypeOperator.IMPLICIT_INTEGER_COERCION
        IrTypeOperator.SAFE_CAST ->
            ProtoTypeOperator.SAFE_CAST
        IrTypeOperator.INSTANCEOF ->
            ProtoTypeOperator.INSTANCEOF
        IrTypeOperator.NOT_INSTANCEOF ->
            ProtoTypeOperator.NOT_INSTANCEOF
        IrTypeOperator.SAM_CONVERSION ->
            ProtoTypeOperator.SAM_CONVERSION
        IrTypeOperator.IMPLICIT_DYNAMIC_CAST ->
            ProtoTypeOperator.IMPLICIT_DYNAMIC_CAST
        IrTypeOperator.REINTERPRET_CAST ->
            error("Unreachable execution")
    }

    private fun serializeTypeOp(expression: IrTypeOperatorCall): ProtoTypeOp {
        val proto = ProtoTypeOp.newBuilder()
            .setOperator(serializeTypeOperator(expression.operator))
            .setOperand(serializeIrType(expression.typeOperand))
            .setArgument(serializeExpression(expression.argument))
        return proto.build()

    }

    private fun serializeVararg(expression: IrVararg): ProtoVararg {
        val proto = ProtoVararg.newBuilder()
            .setElementType(serializeIrType(expression.varargElementType))
        expression.elements.forEach {
            proto.addElement(serializeVarargElement(it))
        }
        return proto.build()
    }

    private fun serializeVarargElement(element: IrVarargElement): ProtoVarargElement {
        val proto = ProtoVarargElement.newBuilder()
        when (element) {
            is IrExpression
            -> proto.expression = serializeExpression(element)
            is IrSpreadElement
            -> proto.spreadElement = serializeSpreadElement(element)
            else -> error("Unknown vararg element kind")
        }
        return proto.build()
    }

    private fun serializeWhen(expression: IrWhen): ProtoWhen {
        val proto = ProtoWhen.newBuilder()

        expression.origin?.let { proto.setOrigin(serializeIrStatementOrigin(it)) }

        val branches = expression.branches
        branches.forEach {
            proto.addBranch(serializeStatement(it))
        }

        return proto.build()
    }

    private fun serializeLoop(expression: IrLoop): ProtoLoop {
        val proto = ProtoLoop.newBuilder()
            .setCondition(serializeExpression(expression.condition)).apply {
                expression.origin?.let { setOrigin(serializeIrStatementOrigin(it)) }
            }

        expression.label?.let {
            proto.label = serializeString(it)
        }

        proto.loopId = currentLoopIndex
        loopIndex[expression] = currentLoopIndex++

        val body = expression.body
        if (body != null) {
            proto.body = serializeExpression(body)
        }

        return proto.build()
    }

    private fun serializeWhile(expression: IrWhileLoop): ProtoWhile {
        val proto = ProtoWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    private fun serializeDynamicMemberExpression(expression: IrDynamicMemberExpression): ProtoDynamicMemberExpression {
        val proto = ProtoDynamicMemberExpression.newBuilder()
            .setMemberName(serializeString(expression.memberName))
            .setReceiver(serializeExpression(expression.receiver))

        return proto.build()
    }

    private fun serializeDynamicOperatorExpression(expression: IrDynamicOperatorExpression): ProtoDynamicOperatorExpression {
        val proto = ProtoDynamicOperatorExpression.newBuilder()
            .setOperator(serializeDynamicOperator(expression.operator))
            .setReceiver(serializeExpression(expression.receiver))

        expression.arguments.forEach { proto.addArgument(serializeExpression(it)) }

        return proto.build()
    }

    private fun serializeDynamicOperator(operator: IrDynamicOperator) = when (operator) {
        IrDynamicOperator.UNARY_PLUS -> ProtoDynamicOperatorExpression.IrDynamicOperator.UNARY_PLUS
        IrDynamicOperator.UNARY_MINUS -> ProtoDynamicOperatorExpression.IrDynamicOperator.UNARY_MINUS

        IrDynamicOperator.EXCL -> ProtoDynamicOperatorExpression.IrDynamicOperator.EXCL

        IrDynamicOperator.PREFIX_INCREMENT -> ProtoDynamicOperatorExpression.IrDynamicOperator.PREFIX_INCREMENT
        IrDynamicOperator.PREFIX_DECREMENT -> ProtoDynamicOperatorExpression.IrDynamicOperator.PREFIX_DECREMENT

        IrDynamicOperator.POSTFIX_INCREMENT -> ProtoDynamicOperatorExpression.IrDynamicOperator.POSTFIX_INCREMENT
        IrDynamicOperator.POSTFIX_DECREMENT -> ProtoDynamicOperatorExpression.IrDynamicOperator.POSTFIX_DECREMENT

        IrDynamicOperator.BINARY_PLUS -> ProtoDynamicOperatorExpression.IrDynamicOperator.BINARY_PLUS
        IrDynamicOperator.BINARY_MINUS -> ProtoDynamicOperatorExpression.IrDynamicOperator.BINARY_MINUS
        IrDynamicOperator.MUL -> ProtoDynamicOperatorExpression.IrDynamicOperator.MUL
        IrDynamicOperator.DIV -> ProtoDynamicOperatorExpression.IrDynamicOperator.DIV
        IrDynamicOperator.MOD -> ProtoDynamicOperatorExpression.IrDynamicOperator.MOD

        IrDynamicOperator.GT -> ProtoDynamicOperatorExpression.IrDynamicOperator.GT
        IrDynamicOperator.LT -> ProtoDynamicOperatorExpression.IrDynamicOperator.LT
        IrDynamicOperator.GE -> ProtoDynamicOperatorExpression.IrDynamicOperator.GE
        IrDynamicOperator.LE -> ProtoDynamicOperatorExpression.IrDynamicOperator.LE

        IrDynamicOperator.EQEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.EQEQ
        IrDynamicOperator.EXCLEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.EXCLEQ

        IrDynamicOperator.EQEQEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.EQEQEQ
        IrDynamicOperator.EXCLEQEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.EXCLEQEQ

        IrDynamicOperator.ANDAND -> ProtoDynamicOperatorExpression.IrDynamicOperator.ANDAND
        IrDynamicOperator.OROR -> ProtoDynamicOperatorExpression.IrDynamicOperator.OROR

        IrDynamicOperator.EQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.EQ
        IrDynamicOperator.PLUSEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.PLUSEQ
        IrDynamicOperator.MINUSEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.MINUSEQ
        IrDynamicOperator.MULEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.MULEQ
        IrDynamicOperator.DIVEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.DIVEQ
        IrDynamicOperator.MODEQ -> ProtoDynamicOperatorExpression.IrDynamicOperator.MODEQ

        IrDynamicOperator.ARRAY_ACCESS -> ProtoDynamicOperatorExpression.IrDynamicOperator.ARRAY_ACCESS

        IrDynamicOperator.INVOKE -> ProtoDynamicOperatorExpression.IrDynamicOperator.INVOKE
    }

    private fun serializeBreak(expression: IrBreak): ProtoBreak {
        val proto = ProtoBreak.newBuilder()
        expression.label?.let {
            proto.label = serializeString(it)
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeContinue(expression: IrContinue): ProtoContinue {
        val proto = ProtoContinue.newBuilder()
        expression.label?.let {
            proto.label = serializeString(it)
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeExpression(expression: IrExpression): ProtoExpression {
        logger.log { "### serializing Expression: ${ir2string(expression)}" }

        val coordinates = serializeCoordinates(expression.startOffset, expression.endOffset)
        val proto = ProtoExpression.newBuilder()
            .setType(serializeIrType(expression.type))
            .setCoordinates(coordinates)

        val operationProto = ProtoOperation.newBuilder()

        // TODO: make me a visitor.
        when (expression) {
            is IrBlock -> operationProto.block = serializeBlock(expression)
            is IrBreak -> operationProto.`break` = serializeBreak(expression)
            is IrClassReference -> operationProto.classReference = serializeClassReference(expression)
            is IrCall -> operationProto.call = serializeCall(expression)
            is IrConstructorCall -> operationProto.constructorCall = serializeConstructorCall(expression)
            is IrComposite -> operationProto.composite = serializeComposite(expression)
            is IrConst<*> -> operationProto.const = serializeConst(expression)
            is IrContinue -> operationProto.`continue` = serializeContinue(expression)
            is IrDelegatingConstructorCall -> operationProto.delegatingConstructorCall = serializeDelegatingConstructorCall(expression)
            is IrDoWhileLoop -> operationProto.doWhile = serializeDoWhile(expression)
            is IrEnumConstructorCall -> operationProto.enumConstructorCall = serializeEnumConstructorCall(expression)
            is IrFunctionExpression -> operationProto.functionExpression = serializeFunctionExpression(expression)
            is IrFunctionReference -> operationProto.functionReference = serializeFunctionReference(expression)
            is IrGetClass -> operationProto.getClass = serializeGetClass(expression)
            is IrGetField -> operationProto.getField = serializeGetField(expression)
            is IrGetValue -> operationProto.getValue = serializeGetValue(expression)
            is IrGetEnumValue -> operationProto.getEnumValue = serializeGetEnumValue(expression)
            is IrGetObjectValue -> operationProto.getObject = serializeGetObject(expression)
            is IrInstanceInitializerCall -> operationProto.instanceInitializerCall = serializeInstanceInitializerCall(expression)
            is IrLocalDelegatedPropertyReference -> operationProto.localDelegatedPropertyReference = serializeIrLocalDelegatedPropertyReference(expression)
            is IrPropertyReference -> operationProto.propertyReference = serializePropertyReference(expression)
            is IrReturn -> operationProto.`return` = serializeReturn(expression)
            is IrSetField -> operationProto.setField = serializeSetField(expression)
            is IrSetVariable -> operationProto.setVariable = serializeSetVariable(expression)
            is IrStringConcatenation -> operationProto.stringConcat = serializeStringConcat(expression)
            is IrThrow -> operationProto.`throw` = serializeThrow(expression)
            is IrTry -> operationProto.`try` = serializeTry(expression)
            is IrTypeOperatorCall -> operationProto.typeOp = serializeTypeOp(expression)
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

    private fun serializeStatement(statement: IrElement): ProtoStatement {
        logger.log { "### serializing Statement: ${ir2string(statement)}" }

        val coordinates = serializeCoordinates(statement.startOffset, statement.endOffset)
        val proto = ProtoStatement.newBuilder()
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

    private fun serializeIrDeclarationBase(declaration: IrDeclaration) =
        ProtoDeclarationBase.newBuilder()
            .setSymbol(serializeIrSymbol((declaration as IrSymbolOwner).symbol))
            .setCoordinates(serializeCoordinates(declaration.startOffset, declaration.endOffset))
            .addAllAnnotation(serializeAnnotations(declaration.annotations))
            .setOrigin(serializeIrDeclarationOrigin(declaration.origin))
            .build()

    private fun serializeIrValueParameter(parameter: IrValueParameter): ProtoValueParameter {
        val proto = ProtoValueParameter.newBuilder()
            .setBase(serializeIrDeclarationBase(parameter))
            .setName(serializeName(parameter.name))
            .setIndex(parameter.index)
            .setType(serializeIrType(parameter.type))
            .setIsCrossinline(parameter.isCrossinline)
            .setIsNoinline(parameter.isNoinline)

        parameter.varargElementType?.let { proto.setVarargElementType(serializeIrType(it)) }
        parameter.defaultValue?.let { proto.setDefaultValue(serializeIrExpressionBody(it.expression)) }

        return proto.build()
    }

    private fun serializeIrTypeParameter(parameter: IrTypeParameter): ProtoTypeParameter {
        val proto = ProtoTypeParameter.newBuilder()
            .setBase(serializeIrDeclarationBase(parameter))
            .setName(serializeName(parameter.name))
            .setIndex(parameter.index)
            .setVariance(serializeIrTypeVariance(parameter.variance))
            .setIsReified(parameter.isReified)
        parameter.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }
        return proto.build()
    }

    private fun serializeIrTypeParameterContainer(typeParameters: List<IrTypeParameter>): ProtoTypeParameterContainer {
        val proto = ProtoTypeParameterContainer.newBuilder()
        typeParameters.forEach {
            proto.addTypeParameter(serializeIrTypeParameter(it))
        }
        return proto.build()
    }

    private fun serializeIrFunctionBase(function: IrFunction): ProtoFunctionBase {
        val proto = ProtoFunctionBase.newBuilder()
            .setBase(serializeIrDeclarationBase(function))
            .setName(serializeName(function.name))
            .setVisibility(serializeVisibility(function.visibility))
            .setIsInline(function.isInline)
            .setIsExternal(function.isExternal)
            .setIsExpect(function.isExpect)
            .setReturnType(serializeIrType(function.returnType))
            .setTypeParameters(serializeIrTypeParameterContainer(function.typeParameters))

        function.dispatchReceiverParameter?.let { proto.setDispatchReceiver(serializeIrValueParameter(it)) }
        function.extensionReceiverParameter?.let { proto.setExtensionReceiver(serializeIrValueParameter(it)) }
        function.valueParameters.forEach {
            proto.addValueParameter(serializeIrValueParameter(it))
        }
        if (!bodiesOnlyForInlines || function.isInline) {
            function.body?.let { proto.body = serializeIrStatementBody(it) }
        }
        return proto.build()
    }

    private fun serializeModality(modality: Modality) = when (modality) {
        Modality.FINAL -> ProtoModalityKind.FINAL_MODALITY
        Modality.SEALED -> ProtoModalityKind.SEALED_MODALITY
        Modality.OPEN -> ProtoModalityKind.OPEN_MODALITY
        Modality.ABSTRACT -> ProtoModalityKind.ABSTRACT_MODALITY
    }

    private fun serializeIrConstructor(declaration: IrConstructor): ProtoConstructor =
        ProtoConstructor.newBuilder()
            .setBase(serializeIrFunctionBase(declaration))
            .setIsPrimary(declaration.isPrimary)
            .build()

    private fun serializeIrFunction(declaration: IrSimpleFunction): ProtoFunction {
        val proto = ProtoFunction.newBuilder()
            .setBase(serializeIrFunctionBase(declaration))
            .setModality(serializeModality(declaration.modality))
            .setIsTailrec(declaration.isTailrec)
            .setIsSuspend(declaration.isSuspend)
            .setIsFakeOverride(declaration.isFakeOverride)

        declaration.overriddenSymbols.forEach {
            proto.addOverridden(serializeIrSymbol(it))
        }

        return proto.build()
    }

    private fun serializeIrAnonymousInit(declaration: IrAnonymousInitializer) =
        ProtoAnonymousInit.newBuilder()
            .setBase(serializeIrDeclarationBase(declaration))
            .setBody(serializeIrStatementBody(declaration.body))
            .build()

    private fun serializeIrLocalDelegatedProperty(variable: IrLocalDelegatedProperty): ProtoLocalDelegatedProperty {
        val proto = ProtoLocalDelegatedProperty.newBuilder()
            .setBase(serializeIrDeclarationBase(variable))
            .setName(serializeString(variable.name.toString()))
            .setIsVar(variable.isVar)
            .setType(serializeIrType(variable.type))
            .setDelegate(serializeIrVariable(variable.delegate))
            .setGetter(serializeIrFunction(variable.getter as IrSimpleFunction)) // TODO: can it be non simple?

        variable.setter?.let { proto.setSetter(serializeIrFunction(it as IrSimpleFunction)) } // TODO: ditto.

        return proto.build()
    }

    private fun serializeIrProperty(property: IrProperty): ProtoProperty {
        declarationTable.uniqIdByDeclaration(property)

        val proto = ProtoProperty.newBuilder()
            .setBase(serializeIrDeclarationBase(property))
            .setIsDelegated(property.isDelegated)
            .setName(serializeName(property.name))
            .setVisibility(serializeVisibility(property.visibility))
            .setModality(serializeModality(property.modality))
            .setIsVar(property.isVar)
            .setIsConst(property.isConst)
            .setIsLateinit(property.isLateinit)
            .setIsDelegated(property.isDelegated)
            .setIsExternal(property.isExternal)
            .setIsExpect(property.isExpect)
            .setIsFakeOverride(property.isFakeOverride)

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

    private fun serializeIrField(field: IrField): ProtoField {
        val proto = ProtoField.newBuilder()
            .setBase(serializeIrDeclarationBase(field))
            .setName(serializeName(field.name))
            .setVisibility(serializeVisibility(field.visibility))
            .setIsFinal(field.isFinal)
            .setIsExternal(field.isExternal)
            .setIsStatic(field.isStatic)
            .setType(serializeIrType(field.type))
            .setIsFakeOverride(field.isFakeOverride)
        val initializer = field.initializer?.expression
        if (initializer != null) {
            proto.initializer = serializeIrExpressionBody(initializer)
        }
        return proto.build()
    }

    private fun serializeIrVariable(variable: IrVariable): ProtoVariable {
        val proto = ProtoVariable.newBuilder()
            .setBase(serializeIrDeclarationBase(variable))
            .setName(serializeName(variable.name))
            .setType(serializeIrType(variable.type))
            .setIsConst(variable.isConst)
            .setIsVar(variable.isVar)
            .setIsLateinit(variable.isLateinit)
        variable.initializer?.let { proto.initializer = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeIrDeclarationContainer(declarations: List<IrDeclaration>): ProtoDeclarationContainer {
        val proto = ProtoDeclarationContainer.newBuilder()
        declarations.forEach {
            //if (it is IrDeclarationWithVisibility && it.visibility == Visibilities.INVISIBLE_FAKE) return@forEach
            proto.addDeclaration(serializeDeclaration(it))
        }
        return proto.build()
    }

    private fun serializeClassKind(kind: ClassKind) = when (kind) {
        CLASS -> ProtoClassKind.CLASS
        INTERFACE -> ProtoClassKind.INTERFACE
        ENUM_CLASS -> ProtoClassKind.ENUM_CLASS
        ENUM_ENTRY -> ProtoClassKind.ENUM_ENTRY
        ANNOTATION_CLASS -> ProtoClassKind.ANNOTATION_CLASS
        OBJECT -> ProtoClassKind.OBJECT
    }

    private fun serializeIrClass(clazz: IrClass): ProtoClass {
        val proto = ProtoClass.newBuilder()
            .setBase(serializeIrDeclarationBase(clazz))
            .setName(serializeName(clazz.name))
            .setKind(serializeClassKind(clazz.kind))
            .setVisibility(serializeVisibility(clazz.visibility))
            .setModality(serializeModality(clazz.modality))
            .setIsCompanion(clazz.isCompanion)
            .setIsInner(clazz.isInner)
            .setIsData(clazz.isData)
            .setIsExternal(clazz.isExternal)
            .setIsInline(clazz.isInline)
            .setIsExpect(clazz.isExpect)
            .setTypeParameters(serializeIrTypeParameterContainer(clazz.typeParameters))
            .setDeclarationContainer(serializeIrDeclarationContainer(clazz.declarations))
        clazz.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }
        clazz.thisReceiver?.let { proto.thisReceiver = serializeIrValueParameter(it) }

        return proto.build()
    }

    private fun serializeIrTypeAlias(typeAlias: IrTypeAlias): ProtoTypeAlias =
        ProtoTypeAlias.newBuilder()
            .setBase(serializeIrDeclarationBase(typeAlias))
            .setName(serializeName(typeAlias.name))
            .setVisibility(serializeVisibility(typeAlias.visibility))
            .setTypeParameters(serializeIrTypeParameterContainer(typeAlias.typeParameters))
            .setExpandedType(serializeIrType(typeAlias.expandedType))
            .setIsActual(typeAlias.isActual)
            .build()

    private fun serializeIrEnumEntry(enumEntry: IrEnumEntry): ProtoEnumEntry {
        val proto = ProtoEnumEntry.newBuilder()
            .setBase(serializeIrDeclarationBase(enumEntry))
            .setName(serializeName(enumEntry.name))

        enumEntry.initializerExpression?.let {
            proto.initializer = serializeIrExpressionBody(it)
        }
        enumEntry.correspondingClass?.let {
            proto.correspondingClass = serializeIrClass(it)
        }
        return proto.build()
    }

    private fun serializeDeclaration(declaration: IrDeclaration): ProtoDeclaration {
        logger.log { "### serializing Declaration: ${ir2string(declaration)}" }

        val proto = ProtoDeclaration.newBuilder()

        when (declaration) {
            is IrAnonymousInitializer ->
                proto.irAnonymousInit = serializeIrAnonymousInit(declaration)
            is IrConstructor ->
                proto.irConstructor = serializeIrConstructor(declaration)
            is IrField ->
                proto.irField = serializeIrField(declaration)
            is IrSimpleFunction ->
                proto.irFunction = serializeIrFunction(declaration)
            is IrTypeParameter ->
                proto.irTypeParameter = serializeIrTypeParameter(declaration)
            is IrVariable ->
                proto.irVariable = serializeIrVariable(declaration)
            is IrValueParameter ->
                proto.irValueParameter = serializeIrValueParameter(declaration)
            is IrClass ->
                proto.irClass = serializeIrClass(declaration)
            is IrEnumEntry ->
                proto.irEnumEntry = serializeIrEnumEntry(declaration)
            is IrProperty ->
                proto.irProperty = serializeIrProperty(declaration)
            is IrLocalDelegatedProperty ->
                proto.irLocalDelegatedProperty = serializeIrLocalDelegatedProperty(declaration)
            is IrTypeAlias ->
                proto.irTypeAlias = serializeIrTypeAlias(declaration)
            else ->
                TODO("Declaration serialization not supported yet: $declaration")
        }

        return proto.build()
    }

// ---------- Top level ------------------------------------------------------

    private fun serializeFileEntry(entry: SourceManager.FileEntry): ProtoFileEntry = ProtoFileEntry.newBuilder()
        .setName(entry.name)
        .addAllLineStartOffsets(entry.lineStartOffsets.asIterable())
        .build()

    open fun backendSpecificExplicitRoot(declaration: IrFunction) = false
    open fun backendSpecificExplicitRoot(declaration: IrClass) = false

    fun serializeIrFile(file: IrFile): SerializedIrFile {
        val topLevelDeclarations = mutableListOf<SerializedDeclaration>()

        val proto = ProtoFile.newBuilder()
            .setFileEntry(serializeFileEntry(file.fileEntry))
            .addAllFqName(serializeFqName(file.fqName))
            .addAllAnnotation(serializeAnnotations(file.annotations))

        file.declarations.forEach {
            if (it.descriptor.isExpectMember && !it.descriptor.isSerializableExpectClass) {
                topLevelDeclarations.add(SkippedDeclaration)
                return@forEach
            }

            val byteArray = serializeDeclaration(it).toByteArray()
            val uniqId = declarationTable.uniqIdByDeclaration(it)
            topLevelDeclarations.add(TopLevelDeclaration(uniqId.index, uniqId.isLocal, it.descriptor.toString(), byteArray))
            if (uniqId.isPublic) {
                proto.addDeclarationId(uniqId.index)
            }
        }

        // TODO: is it Konan specific?

        // Make sure that all top level properties are initialized on library's load.
        file.declarations
            .filterIsInstance<IrProperty>()
            .filter { it.backingField?.initializer != null }
            .forEach { proto.addExplicitlyExportedToCompiler(serializeIrSymbol(it.backingField!!.symbol)) }

        // TODO: Konan specific

        file.acceptVoid(object : IrElementVisitorVoid {
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

        return SerializedIrFile(
            proto.build().toByteArray(),
            file.fqName.asString(),
            file.path,
            IrMemoryArrayWriter(protoSymbolArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryArrayWriter(protoTypeArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryArrayWriter(protoStringArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryArrayWriter(protoBodyArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryDeclarationWriter(topLevelDeclarations).writeIntoMemory()
        )
    }
}
