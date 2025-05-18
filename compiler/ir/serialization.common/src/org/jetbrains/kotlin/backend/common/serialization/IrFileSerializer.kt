/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleTypeNullability
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel.ABI_LEVEL_2_2
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.SerializedDeclaration
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.impl.IrArrayWriter
import org.jetbrains.kotlin.library.impl.IrDeclarationWriter
import org.jetbrains.kotlin.library.impl.IrStringWriter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import java.io.File
import org.jetbrains.kotlin.backend.common.serialization.proto.FieldAccessCommon as ProtoFieldAccessCommon
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
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
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDelegatingConstructorCall as ProtoDelegatingConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDoWhile as ProtoDoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicMemberExpression as ProtoDynamicMemberExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicOperatorExpression as ProtoDynamicOperatorExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicType as ProtoDynamicType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumConstructorCall as ProtoEnumConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumEntry as ProtoEnumEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorCallExpression as ProtoErrorCallExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorExpression as ProtoErrorExpression
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
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInlineClassRepresentation as ProtoIrInlineClassRepresentation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInlinedFunctionBlock as ProtoInlinedFunctionBlock
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInstanceInitializerCall as ProtoInstanceInitializerCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedProperty as ProtoLocalDelegatedProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedPropertyReference as ProtoLocalDelegatedPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation as ProtoIrMultiFieldValueClassRepresentation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrOperation as ProtoOperation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrPropertyReference as ProtoPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturn as ProtoReturn
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturnableBlock as ProtoReturnableBlock
import org.jetbrains.kotlin.backend.common.serialization.proto.IrRichFunctionReference as ProtoRichFunctionReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrRichPropertyReference as ProtoRichPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetField as ProtoSetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetValue as ProtoSetValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleType as ProtoSimpleType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSpreadElement as ProtoSpreadElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStringConcat as ProtoStringConcat
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBody as ProtoSyntheticBody
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBodyKind as ProtoSyntheticBodyKind
import org.jetbrains.kotlin.backend.common.serialization.proto.IrThrow as ProtoThrow
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTry as ProtoTry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAbbreviation as ProtoTypeAbbreviation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAlias as ProtoTypeAlias
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOp as ProtoTypeOp
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOperator as ProtoTypeOperator
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter as ProtoTypeParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoValueParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVararg as ProtoVararg
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVarargElement as ProtoVarargElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVariable as ProtoVariable
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhen as ProtoWhen
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhile as ProtoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.Loop as ProtoLoop
import org.jetbrains.kotlin.backend.common.serialization.proto.MemberAccessCommon as ProtoMemberAccessCommon
import org.jetbrains.kotlin.backend.common.serialization.proto.NullableIrExpression as ProtoNullableIrExpression

open class IrFileSerializer(
    private val settings: IrSerializationSettings,
    private val declarationTable: DeclarationTable<*>,
) {
    private val loopIndex = hashMapOf<IrLoop, Int>()
    private var currentLoopIndex = 0

    /**
     * The abstraction that represents all [ProtoType]s to be serialized in the current [IrFile].
     *
     * It writes every protobuf 'type' entity to a byte array and ensures that there is no other 'type'
     * entity that has been already added with exactly the same bytes. In other words, ensures that
     * all types added to [ProtoTypeArray] are indeed unique. This is useful when serializing a mix
     * of bound and unbound IR, when there are two equivalent [IrType]s referencing different symbols
     * (bound and unbound) - the situation that the serializer otherwise is not able to recognize
     * and therefore serializes them as two distinct types.
     */
    protected class ProtoTypeArray {
        private class UniqueType(val protoType: ProtoType) {
            val byteArray: ByteArray = protoType.toByteArray()

            private val memoizedHashCode = byteArray.contentHashCode()
            override fun hashCode() = memoizedHashCode

            override fun equals(other: Any?) =
                other is UniqueType && other.memoizedHashCode == memoizedHashCode && other.byteArray contentEquals byteArray
        }

        // Use `LinkedHashMap` to keep only unique types but preserve the insertion order.
        private val uniqueTypes: MutableMap<UniqueType, /* ordered index */ Int> = linkedMapOf()

        val protoTypes: List<ProtoType> get() = uniqueTypes.keys.map { it.protoType }
        val byteArrays: List<ByteArray> get() = uniqueTypes.keys.map { it.byteArray }

        fun addAndGetIndex(protoType: ProtoType) = uniqueTypes.getOrPut(UniqueType(protoType)) { uniqueTypes.size }
    }

    /** The same type can be used multiple times in a file, so use this map to store the unique index by a deduplication key. */
    private val protoTypeMap = hashMapOf<IrTypeDeduplicationKey, /* unique type index */ Int>()
    protected val protoTypeArray = ProtoTypeArray()

    private val protoStringMap = hashMapOf<String, Int>()
    protected val protoStringArray = arrayListOf<String>()

    private val protoIrFileEntryMap = hashMapOf<ProtoFileEntryDeduplicationKey, Int>()
    protected val protoIrFileEntryArray = arrayListOf<ProtoFileEntry>()

    // The same signature could be used multiple times in a file
    // so use this index to store signature only once.
    private val protoIdSignatureMap = mutableMapOf<IdSignature, Int>()
    protected val protoIdSignatureArray = arrayListOf<ProtoIdSignature>()
    private val idSignatureSerializer = IdSignatureSerializer(
        ::serializeString,
        ::serializeDebugInfo,
        protoIdSignatureMap,
        protoIdSignatureArray
    )

    protected val protoBodyArray = mutableListOf<XStatementOrExpression>()

    protected val protoDebugInfoMap = hashMapOf<String, Int>()
    protected val protoDebugInfoArray = arrayListOf<String>()

    private val preprocessedToOriginalInlineFunctions = mutableMapOf<IrSimpleFunction, IrSimpleFunction>()

    private var isInsideInline: Boolean = false
    private var fileContainsInline = false

    interface FileBackendSpecificMetadata {
        fun toByteArray(): ByteArray
    }

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

    private fun serializeIrDeclarationOrigin(origin: IrDeclarationOrigin): Int = serializeString(origin.name)

    private inline fun serializeIrStatementOrigin(origin: IrStatementOrigin?, saveOriginIndex: (Int) -> Unit) {
        if (origin == null) {
            // Nothing to serialize.
            return
        }

        if (IrStatementOrigin.IMPLICIT_ARGUMENT == origin && settings.abiCompatibilityLevel == KlibAbiCompatibilityLevel.ABI_LEVEL_2_1) {
            // Kotlin compiler version 2.1.x fails in an attempt to deserialize unknown statement origins.
            // So, as a workaround, we try to avoid serializing such statements when exporting to KLIB ABI level 2.1.
            // For details, see KT-76131, KT-75624, KT-75393.
            return
        }

        val originIndex = serializeString(origin.debugName)
        saveOriginIndex(originIndex)
    }

    private fun serializeCoordinates(start: Int, end: Int): Long =
        if (settings.publicAbiOnly && !isInsideInline) 0 else BinaryCoordinates.encode(start, end)

    /* ------- Strings ---------------------------------------------------------- */

    private fun serializeString(value: String): Int = protoStringMap.getOrPut(value) {
        protoStringArray.add(value)
        protoStringArray.size - 1
    }

    private fun serializeDebugInfo(value: String): Int = protoDebugInfoMap.getOrPut(value) {
        protoDebugInfoArray.add(value)
        protoDebugInfoArray.size - 1
    }

    private fun serializeName(name: Name): Int = serializeString(name.toString())

    /* ------- IrSymbols -------------------------------------------------------- */

    companion object {
        fun protoSymbolKind(symbol: IrSymbol): BinarySymbolData.SymbolKind = when (symbol) {
            is IrAnonymousInitializerSymbol ->
                BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL
            is IrClassSymbol ->
                BinarySymbolData.SymbolKind.CLASS_SYMBOL
            is IrConstructorSymbol ->
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL
            is IrTypeParameterSymbol ->
                BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL
            is IrEnumEntrySymbol ->
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL
            is IrVariableSymbol ->
                BinarySymbolData.SymbolKind.VARIABLE_SYMBOL
            is IrValueParameterSymbol ->
                if (symbol.descriptor is ReceiverParameterDescriptor) // TODO: we use descriptor here.
                    BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL
                else
                    BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL
            is IrSimpleFunctionSymbol ->
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
            is IrReturnableBlockSymbol ->
                BinarySymbolData.SymbolKind.RETURNABLE_BLOCK_SYMBOL
            is IrFieldSymbol ->
                if (symbol.owner.correspondingPropertySymbol?.owner.let {
                        it == null || it.isDelegated || it.getter == null && it.setter == null
                    })
                    BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL
                else
                    BinarySymbolData.SymbolKind.FIELD_SYMBOL
            is IrPropertySymbol ->
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
            is IrLocalDelegatedPropertySymbol ->
                BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL
            is IrTypeAliasSymbol ->
                BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
            is IrFileSymbol ->
                BinarySymbolData.SymbolKind.FILE_SYMBOL
            else ->
                TODO("Unexpected symbol kind: $symbol")
        }
    }

    private fun serializeIrSymbol(symbol: IrSymbol, isDeclared: Boolean = false): Long {
        val signature: IdSignature = runIf(settings.reuseExistingSignaturesForSymbols) { symbol.signature }
            ?: when (symbol) {
                is IrFileSymbol -> IdSignature.FileSignature(symbol) // TODO: special signature for files?
                else -> {
                    val symbolOwner = symbol.owner

                    // Compute the signature:
                    when {
                        symbolOwner is IrDeclaration -> declarationTable.signatureByDeclaration(
                            declaration = symbolOwner,
                            compatibleMode = false,
                            recordInSignatureClashDetector = isDeclared
                        )

                        symbolOwner is IrReturnableBlock && settings.abiCompatibilityLevel.isAtLeast(ABI_LEVEL_2_2) ->
                            declarationTable.signatureByReturnableBlock(symbolOwner)

                        else -> error("Expected symbol owner: ${symbolOwner.render()}")
                    }
                }
            }

        val signatureId = idSignatureSerializer.protoIdSignature(signature)
        val symbolKind = protoSymbolKind(symbol)

        return BinarySymbolData.encode(symbolKind, signatureId)
    }

    /* ------- IrTypes ---------------------------------------------------------- */

    // Serializes all annotations, even having SOURCE retention, since they might be needed in backends, like @Volatile
    private fun serializeAnnotations(annotations: List<IrConstructorCall>) =
        annotations.map {
            for (param in it.arguments) {
                if (param != null) {
                    require(param.isValidConstantAnnotationArgument()) {
                        "This is a compiler bug, please report it to https://kotl.in/issue : parameter value of an annotation constructor must be a const:\nCALL: ${it.render()}\nPARAM: ${param.render()}"
                    }
                }
            }
            serializeConstructorCall(it)
        }

    private fun serializeFqName(fqName: String): List<Int> = fqName.split(".").map(::serializeString)

    private fun serializeIrStarProjection() = BinaryTypeProjection.STAR_CODE

    private fun serializeIrTypeProjection(argument: IrTypeProjection) =
        BinaryTypeProjection
            .encodeType(argument.variance, serializeIrType(argument.type))

    private fun serializeTypeArgument(argument: IrTypeArgument): Long {
        return when (argument) {
            is IrStarProjection -> serializeIrStarProjection()
            is IrTypeProjection -> serializeIrTypeProjection(argument)
        }
    }

    private fun serializeNullability(nullability: SimpleTypeNullability) = when (nullability) {
        SimpleTypeNullability.MARKED_NULLABLE -> IrSimpleTypeNullability.MARKED_NULLABLE
        SimpleTypeNullability.NOT_SPECIFIED -> IrSimpleTypeNullability.NOT_SPECIFIED
        SimpleTypeNullability.DEFINITELY_NOT_NULL -> IrSimpleTypeNullability.DEFINITELY_NOT_NULL
    }

    private fun serializeSimpleType(type: IrSimpleType): ProtoSimpleType {
        val proto = ProtoSimpleType.newBuilder()
            .addAllAnnotation(serializeAnnotations(type.annotations))
            .setClassifier(serializeIrSymbol(type.classifier))
        if (type.nullability != SimpleTypeNullability.NOT_SPECIFIED) {
            proto.setNullability(serializeNullability(type.nullability))
        }
        type.abbreviation?.let { ta ->
            proto.setAbbreviation(serializeIrTypeAbbreviation(ta))
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

    private fun serializeIrTypeData(type: IrType): ProtoType {
        val proto = ProtoType.newBuilder()
        when (type) {
            is IrSimpleType ->
                proto.simple = serializeSimpleType(type)
            is IrDynamicType ->
                proto.dynamic = serializeDynamicType(type)
            is IrErrorType ->
                error("Serialization of IrErrorType is not supported anymore")
        }
        return proto.build()
    }

    private enum class IrTypeKind {
        SIMPLE,
        DYNAMIC,
    }

    private enum class IrTypeArgumentKind {
        STAR,
        PROJECTION
    }

    /**
     * This is just an [IrType] repacked as a data class, good to address a hash map.
     *
     * Note: This key does not guarantee the uniqueness of IR types for several reasons:
     * - [IrTypeDeduplicationKey.classifier] is a symbol of a classifier in a type. In the case of a mix of
     *   bound IR produced by Fir2Ir and unbound IR obtained through deserialization of inline function
     *   body, there can be two symbols (one bound without a signature and another unbound but with a signature)
     *   both pointing effectively to the same declaration.
     * - [IrTypeDeduplicationKey.annotations] is just a list of [IrConstructorCall]s that cannot be
     *   fully compared: The [IrConstructorCallImpl.equals] function resolves to [Any.equals], which
     *   compares only object references.
     * - [IrTypeDeduplicationKey.abbreviation] is just another IR node: [IrTypeAbbreviation]. And it also
     *   cannot be fully compared.
     *
     * However, [IrTypeDeduplicationKey] can be used as a good approximation to store lesser number of records
     * in [protoTypeMap] and overall speed-up the process of types serialization.
     */
    private data class IrTypeDeduplicationKey(
        val kind: IrTypeKind,
        val classifier: IrClassifierSymbol?,
        val nullability: SimpleTypeNullability?,
        val arguments: List<IrTypeArgumentDeduplicationKey>?,
        val annotations: List<IrConstructorCall>,
        val abbreviation: IrTypeAbbreviation?,
    )

    private data class IrTypeArgumentDeduplicationKey(
        val kind: IrTypeArgumentKind,
        val variance: Variance?,
        val type: IrTypeDeduplicationKey?,
    )

    private val IrType.toIrTypeDeduplicationKey: IrTypeDeduplicationKey
        get() {
            val type = this
            return IrTypeDeduplicationKey(
                kind = when (this) {
                    is IrSimpleType -> IrTypeKind.SIMPLE
                    is IrDynamicType -> IrTypeKind.DYNAMIC
                    is IrErrorType -> error("Serialization of IrErrorType is not supported anymore")
                },
                classifier = type.classifierOrNull,
                nullability = (type as? IrSimpleType)?.nullability,
                arguments = (type as? IrSimpleType)?.arguments?.map { it.toIrTypeArgumentDeduplicationKey },
                annotations = type.annotations,
                abbreviation = (type as? IrSimpleType)?.abbreviation
            )
        }

    private val IrTypeArgument.toIrTypeArgumentDeduplicationKey: IrTypeArgumentDeduplicationKey
        get() = IrTypeArgumentDeduplicationKey(
            kind = when (this) {
                is IrStarProjection -> IrTypeArgumentKind.STAR
                is IrTypeProjection -> IrTypeArgumentKind.PROJECTION
            },
            variance = (this as? IrTypeProjection)?.variance,
            type = (this as? IrTypeProjection)?.type?.toIrTypeDeduplicationKey
        )

    private fun serializeIrType(type: IrType) = protoTypeMap.getOrPut(type.toIrTypeDeduplicationKey) {
        protoTypeArray.addAndGetIndex(serializeIrTypeData(type))
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

        serializeIrStatementOrigin(block.origin, proto::setOriginName)

        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeReturnableBlock(returnableBlock: IrReturnableBlock): ProtoReturnableBlock {
        requireAbiAtLeast(ABI_LEVEL_2_2) { returnableBlock }

        val proto = ProtoReturnableBlock.newBuilder()
        proto.symbol = serializeIrSymbol(returnableBlock.symbol)
        proto.base = serializeBlock(returnableBlock)
        return proto.build()
    }

    private fun serializeInlinedFunctionBlock(inlinedFunctionBlock: IrInlinedFunctionBlock): ProtoInlinedFunctionBlock {
        requireAbiAtLeast(ABI_LEVEL_2_2) { inlinedFunctionBlock }

        val proto = ProtoInlinedFunctionBlock.newBuilder()
        inlinedFunctionBlock.inlinedFunctionSymbol?.let { proto.setInlinedFunctionSymbol(serializeIrSymbol(it)) }
        proto.inlinedFunctionFileEntryId = serializeFileEntryId(inlinedFunctionBlock.inlinedFunctionFileEntry)
        proto.base = serializeBlock(inlinedFunctionBlock)
        proto.inlinedFunctionStartOffset = inlinedFunctionBlock.inlinedFunctionStartOffset
        proto.inlinedFunctionEndOffset = inlinedFunctionBlock.inlinedFunctionEndOffset
        return proto.build()
    }

    private fun serializeComposite(composite: IrComposite): ProtoComposite {
        val proto = ProtoComposite.newBuilder()

        serializeIrStatementOrigin(composite.origin, proto::setOriginName)
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

    private fun serializeMemberAccessCommon(call: IrMemberAccessExpression<*>): ProtoMemberAccessCommon {
        fun buildProtoNullableIrExpression(arg: IrExpression?): ProtoNullableIrExpression.Builder {
            val argOrNullProto = ProtoNullableIrExpression.newBuilder()
            if (arg == null) {
                // Am I observing an IR generation regression?
                // I see a lack of arg for an empty vararg,
                // rather than an empty vararg node.

                // TODO: how do we assert that without descriptor?
                //assert(it.varargElementType != null || it.hasDefaultValue())
            } else {
                argOrNullProto.expression = serializeExpression(arg)
            }
            return argOrNullProto
        }

        val proto = ProtoMemberAccessCommon.newBuilder()

        if (settings.abiCompatibilityLevel.isAtLeast(ABI_LEVEL_2_2)) {
            for (arg in call.arguments) {
                proto.addArgument(buildProtoNullableIrExpression(arg))
            }
        } else { // KLIB ABI 2.1:
            val callableSymbol = call.symbol
            require(callableSymbol.isBound) { callableSymbol }

            for ((parameter, arg) in call.getAllArgumentsWithIr()) {
                when (parameter.kind) {
                    IrParameterKind.DispatchReceiver -> if (arg != null) proto.dispatchReceiver = serializeExpression(arg)
                    IrParameterKind.ExtensionReceiver -> if (arg != null) proto.extensionReceiver = serializeExpression(arg)
                    IrParameterKind.Context -> serializationNotSupportedAtCurrentAbiLevel({ "Context parameter" }) { callableSymbol.owner }
                    IrParameterKind.Regular -> proto.addRegularArgument(buildProtoNullableIrExpression(arg))
                }
            }
        }

        for (typeArg in call.typeArguments) {
            // See `ForbidUsingExtensionPropertyTypeParameterInDelegate` language feature
            val typeArgumentIndex = typeArg?.let { serializeIrType(it) } ?: -1
            proto.addTypeArgument(typeArgumentIndex)
        }

        return proto.build()
    }

    private fun serializeCall(call: IrCall): ProtoCall {
        val proto = ProtoCall.newBuilder()
        proto.symbol = serializeIrSymbol(call.symbol)
        serializeIrStatementOrigin(call.origin, proto::setOriginName)

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
            serializeIrStatementOrigin(call.origin, ::setOriginName)
        }.build()

    private fun serializeFunctionExpression(functionExpression: IrFunctionExpression): ProtoFunctionExpression =
        ProtoFunctionExpression.newBuilder().apply {
            function = serializeIrFunction(functionExpression.function)
            serializeIrStatementOrigin(functionExpression.origin, ::setOriginName)
        }.build()

    private fun serializeFunctionReference(callable: IrFunctionReference): ProtoFunctionReference {
        val proto = ProtoFunctionReference.newBuilder()
            .setSymbol(serializeIrSymbol(callable.symbol))
            .setMemberAccess(serializeMemberAccessCommon(callable))

        callable.reflectionTarget?.let { proto.reflectionTargetSymbol = serializeIrSymbol(it) }
        serializeIrStatementOrigin(callable.origin, proto::setOriginName)
        return proto.build()
    }

    private fun serializeRichFunctionReference(callable: IrRichFunctionReference): ProtoRichFunctionReference {
        requireAbiAtLeast(ABI_LEVEL_2_2) { callable }

        return ProtoRichFunctionReference.newBuilder().apply {
            callable.reflectionTargetSymbol?.let { reflectionTargetSymbol = serializeIrSymbol(it) }
            overriddenFunctionSymbol = serializeIrSymbol(callable.overriddenFunctionSymbol)
            for (boundValue in callable.boundValues) {
                addBoundValues(serializeExpression(boundValue))
            }
            invokeFunction = serializeIrFunction(callable.invokeFunction)
            serializeIrStatementOrigin(callable.origin, ::setOriginName)
            flags = RichFunctionReferenceFlags.encode(callable)
        }.build()
    }

    private fun serializeRichPropertyReference(callable: IrRichPropertyReference): ProtoRichPropertyReference {
        requireAbiAtLeast(ABI_LEVEL_2_2) { callable }

        return ProtoRichPropertyReference.newBuilder().apply {
            callable.reflectionTargetSymbol?.let { reflectionTargetSymbol = serializeIrSymbol(it) }
            for (boundValue in callable.boundValues) {
                addBoundValues(serializeExpression(boundValue))
            }
            getterFunction = serializeIrFunction(callable.getterFunction)
            callable.setterFunction?.let { setterFunction = serializeIrFunction(it) }
            serializeIrStatementOrigin(callable.origin, ::setOriginName)
        }.build()
    }

    private fun serializeIrLocalDelegatedPropertyReference(
        callable: IrLocalDelegatedPropertyReference,
    ): ProtoLocalDelegatedPropertyReference {
        val proto = ProtoLocalDelegatedPropertyReference.newBuilder()
            .setDelegate(serializeIrSymbol(callable.delegate))
            .setGetter(serializeIrSymbol(callable.getter))
            .setSymbol(serializeIrSymbol(callable.symbol))

        serializeIrStatementOrigin(callable.origin, proto::setOriginName)
        callable.setter?.let { proto.setSetter(serializeIrSymbol(it)) }

        return proto.build()
    }

    private fun serializePropertyReference(callable: IrPropertyReference): ProtoPropertyReference {
        val proto = ProtoPropertyReference.newBuilder()
            .setMemberAccess(serializeMemberAccessCommon(callable))
            .setSymbol(serializeIrSymbol(callable.symbol))

        serializeIrStatementOrigin(callable.origin, proto::setOriginName)
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

    private fun serializeConst(value: IrConst): ProtoConst {
        val proto = ProtoConst.newBuilder()
        when (value.kind) {
            IrConstKind.Null -> proto.`null` = true
            IrConstKind.Boolean -> proto.boolean = value.value as Boolean
            IrConstKind.Byte -> proto.byte = (value.value as Byte).toInt()
            IrConstKind.Char -> proto.char = (value.value as Char).code
            IrConstKind.Short -> proto.short = (value.value as Short).toInt()
            IrConstKind.Int -> proto.int = value.value as Int
            IrConstKind.Long -> proto.long = value.value as Long
            IrConstKind.String -> proto.string = serializeString(value.value as String)
            IrConstKind.Float -> proto.floatBits = (value.value as Float).toBits()
            IrConstKind.Double -> proto.doubleBits = (value.value as Double).toBits()
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
                serializeIrStatementOrigin(expression.origin, ::setOriginName)
            }
            .build()

    private fun serializeGetValue(expression: IrGetValue): ProtoGetValue =
        ProtoGetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol)).apply {
                serializeIrStatementOrigin(expression.origin, ::setOriginName)
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

    private fun serializeReturn(operation: ProtoOperation.Builder, expression: IrReturn) {
        val proto = ProtoReturn.newBuilder()
            .setReturnTarget(serializeIrSymbol(expression.returnTargetSymbol))
            .setValue(serializeExpression(expression.value))
        operation.`return` = proto.build()
    }

    private fun serializeSetField(expression: IrSetField): ProtoSetField =
        ProtoSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value)).apply {
                serializeIrStatementOrigin(expression.origin, ::setOriginName)
            }
            .build()

    private fun serializeSetValue(expression: IrSetValue): ProtoSetValue =
        ProtoSetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
            .setValue(serializeExpression(expression.value)).apply {
                serializeIrStatementOrigin(expression.origin, ::setOriginName)
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
                IrSyntheticBodyKind.ENUM_ENTRIES -> ProtoSyntheticBodyKind.ENUM_ENTRIES
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
            ProtoTypeOperator.REINTERPRET_CAST
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
            is IrExpression,
                -> proto.expression = serializeExpression(element)
            is IrSpreadElement,
                -> proto.spreadElement = serializeSpreadElement(element)
            else -> error("Unknown vararg element kind")
        }
        return proto.build()
    }

    private fun serializeWhen(expression: IrWhen): ProtoWhen {
        val proto = ProtoWhen.newBuilder()

        serializeIrStatementOrigin(expression.origin, proto::setOriginName)

        val branches = expression.branches
        branches.forEach {
            proto.addBranch(serializeStatement(it))
        }

        return proto.build()
    }

    private fun serializeLoop(expression: IrLoop): ProtoLoop {
        val loopIdx = currentLoopIndex++
        loopIndex[expression] = loopIdx

        val proto = ProtoLoop.newBuilder()
            .setCondition(serializeExpression(expression.condition)).apply {
                serializeIrStatementOrigin(expression.origin, ::setOriginName)
            }

        expression.label?.let {
            proto.label = serializeString(it)
        }

        proto.loopId = loopIdx

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

    private fun serializeErrorExpression(expression: IrErrorExpression): ProtoErrorExpression {
        val proto = ProtoErrorExpression.newBuilder().setDescription(serializeString(expression.description))
        return proto.build()
    }

    private fun serializeErrorCallExpression(callExpression: IrErrorCallExpression): ProtoErrorCallExpression {
        val proto = ProtoErrorCallExpression.newBuilder().setDescription(serializeString(callExpression.description))
        callExpression.explicitReceiver?.let {
            proto.setReceiver(serializeExpression(it))
        }
        callExpression.arguments.forEach {
            proto.addValueArgument(serializeExpression(it))
        }
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
        val loopId = loopIndex[expression.loop] ?: -1
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeContinue(expression: IrContinue): ProtoContinue {
        val proto = ProtoContinue.newBuilder()
        expression.label?.let {
            proto.label = serializeString(it)
        }
        val loopId = loopIndex[expression.loop] ?: -1
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeExpression(expression: IrExpression): ProtoExpression {
        val coordinates = serializeCoordinates(expression.startOffset, expression.endOffset)
        val proto = ProtoExpression.newBuilder()
            .setType(serializeIrType(expression.type))
            .setCoordinates(coordinates)

        val operationProto = ProtoOperation.newBuilder()

        // TODO: make me a visitor.
        when (expression) {
            is IrReturnableBlock -> operationProto.returnableBlock = serializeReturnableBlock(expression)
            is IrInlinedFunctionBlock -> operationProto.inlinedFunctionBlock = serializeInlinedFunctionBlock(expression)
            is IrBlock -> operationProto.block = serializeBlock(expression)
            is IrBreak -> operationProto.`break` = serializeBreak(expression)
            is IrClassReference -> operationProto.classReference = serializeClassReference(expression)
            is IrCall -> operationProto.call = serializeCall(expression)
            is IrConstructorCall -> operationProto.constructorCall = serializeConstructorCall(expression)
            is IrComposite -> operationProto.composite = serializeComposite(expression)
            is IrConst -> operationProto.const = serializeConst(expression)
            is IrContinue -> operationProto.`continue` = serializeContinue(expression)
            is IrDelegatingConstructorCall -> operationProto.delegatingConstructorCall = serializeDelegatingConstructorCall(expression)
            is IrDoWhileLoop -> operationProto.doWhile = serializeDoWhile(expression)
            is IrEnumConstructorCall -> operationProto.enumConstructorCall = serializeEnumConstructorCall(expression)
            is IrFunctionExpression -> operationProto.functionExpression = serializeFunctionExpression(expression)
            is IrFunctionReference -> operationProto.functionReference = serializeFunctionReference(expression)
            is IrRichFunctionReference -> operationProto.richFunctionReference = serializeRichFunctionReference(expression)
            is IrRichPropertyReference -> operationProto.richPropertyReference = serializeRichPropertyReference(expression)
            is IrGetClass -> operationProto.getClass = serializeGetClass(expression)
            is IrGetField -> operationProto.getField = serializeGetField(expression)
            is IrGetValue -> operationProto.getValue = serializeGetValue(expression)
            is IrGetEnumValue -> operationProto.getEnumValue = serializeGetEnumValue(expression)
            is IrGetObjectValue -> operationProto.getObject = serializeGetObject(expression)
            is IrInstanceInitializerCall -> operationProto.instanceInitializerCall = serializeInstanceInitializerCall(expression)
            is IrLocalDelegatedPropertyReference -> operationProto.localDelegatedPropertyReference =
                serializeIrLocalDelegatedPropertyReference(expression)
            is IrPropertyReference -> operationProto.propertyReference = serializePropertyReference(expression)
            is IrReturn -> serializeReturn(operationProto, expression)
            is IrSetField -> operationProto.setField = serializeSetField(expression)
            is IrSetValue -> operationProto.setValue = serializeSetValue(expression)
            is IrStringConcatenation -> operationProto.stringConcat = serializeStringConcat(expression)
            is IrThrow -> operationProto.`throw` = serializeThrow(expression)
            is IrTry -> operationProto.`try` = serializeTry(expression)
            is IrTypeOperatorCall -> operationProto.typeOp = serializeTypeOp(expression)
            is IrVararg -> operationProto.vararg = serializeVararg(expression)
            is IrWhen -> operationProto.`when` = serializeWhen(expression)
            is IrWhileLoop -> operationProto.`while` = serializeWhile(expression)
            is IrDynamicMemberExpression -> operationProto.dynamicMember = serializeDynamicMemberExpression(expression)
            is IrDynamicOperatorExpression -> operationProto.dynamicOperator = serializeDynamicOperatorExpression(expression)
            is IrErrorCallExpression -> operationProto.errorCallExpression = serializeErrorCallExpression(expression)
            is IrErrorExpression -> operationProto.errorExpression = serializeErrorExpression(expression)
            else -> error("Expression serialization is not supported yet: ${expression.render()}")
        }
        proto.setOperation(operationProto)

        return proto.build()
    }

    private fun serializeStatement(statement: IrElement): ProtoStatement {

        val coordinates = serializeCoordinates(statement.startOffset, statement.endOffset)
        val proto = ProtoStatement.newBuilder()
            .setCoordinates(coordinates)

        when (statement) {
            is IrDeclaration -> {
                proto.declaration = serializeDeclaration(statement)
            }
            is IrExpression -> {
                proto.expression = serializeExpression(statement)
            }
            is IrBlockBody -> {
                proto.blockBody = serializeBlockBody(statement)
            }
            is IrBranch -> {
                proto.branch = serializeBranch(statement)
            }
            is IrCatch -> {
                proto.catch = serializeCatch(statement)
            }
            is IrSyntheticBody -> {
                proto.syntheticBody = serializeSyntheticBody(statement)
            }
            is IrExpressionBody -> {
                proto.expression = serializeExpression(statement.expression)
            }
            else -> {
                TODO("Statement not implemented yet: ${statement.render()}")
            }
        }
        return proto.build()
    }

    private fun serializeIrDeclarationBase(declaration: IrDeclaration, flags: Long?): ProtoDeclarationBase {
        return with(ProtoDeclarationBase.newBuilder()) {
            symbol = serializeIrSymbol(
                (declaration as IrSymbolOwner).symbol,
                isDeclared = declaration !in preprocessedToOriginalInlineFunctions
            )
            coordinates = serializeCoordinates(declaration.startOffset, declaration.endOffset)
            addAllAnnotation(serializeAnnotations(declaration.annotations))
            flags?.let { setFlags(it) }
            originName = serializeIrDeclarationOrigin(declaration.origin)
            build()
        }
    }

    private fun serializeNameAndType(name: Name, type: IrType): Long {
        val nameIndex = serializeName(name)
        val typeIndex = serializeIrType(type)
        return BinaryNameAndType.encode(nameIndex, typeIndex)
    }

    private fun serializeIrValueParameter(parameter: IrValueParameter): ProtoValueParameter {
        if (parameter.kind == IrParameterKind.Context) {
            requireAbiAtLeast(ABI_LEVEL_2_2, { "Context parameter" }) { parameter.parent }
        }

        val proto = ProtoValueParameter.newBuilder()
            .setBase(serializeIrDeclarationBase(parameter, ValueParameterFlags.encode(parameter)))
            .setNameType(serializeNameAndType(parameter.name, parameter.type))

        parameter.varargElementType?.let { proto.setVarargElementType(serializeIrType(it)) }
        parameter.defaultValue?.let { proto.setDefaultValue(serializeIrExpressionBody(it.expression)) }

        return proto.build()
    }

    private fun serializeIrTypeParameter(parameter: IrTypeParameter): ProtoTypeParameter {
        val proto = ProtoTypeParameter.newBuilder()
            .setBase(serializeIrDeclarationBase(parameter, TypeParameterFlags.encode(parameter)))
            .setName(serializeName(parameter.name))
        parameter.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }

        return proto.build()
    }

    private fun serializeIrFunctionBase(function: IrFunction, flags: Long): ProtoFunctionBase {
        val isInsideInlineBefore = isInsideInline
        isInsideInline = function.isInline || isInsideInlineBefore
        fileContainsInline = fileContainsInline || function.isInline

        val proto = ProtoFunctionBase.newBuilder()
            .setBase(serializeIrDeclarationBase(function, flags))
            .setNameType(serializeNameAndType(function.name, function.returnType))

        function.typeParameters.forEach {
            proto.addTypeParameter(serializeIrTypeParameter(it))
        }

        val isAnnotationClass = ((function as? IrConstructor)?.returnType?.classifierOrNull as? IrClass)?.kind == ClassKind.ANNOTATION_CLASS

        for (parameter in function.parameters) {
            if (isAnnotationClass) {
                require(parameter.isValidConstantAnnotationArgument()) {
                    "This is a compiler bug, please report it to https://kotl.in/issue : default value of annotation construction parameter must have const initializer:\n${parameter.render()}"
                }
            }

            val parameterProto = serializeIrValueParameter(parameter)
            when (parameter.kind) {
                IrParameterKind.DispatchReceiver -> proto.setDispatchReceiver(parameterProto)
                IrParameterKind.Context -> proto.addContextParameter(parameterProto)
                IrParameterKind.ExtensionReceiver -> proto.setExtensionReceiver(parameterProto)
                IrParameterKind.Regular -> proto.addRegularParameter(parameterProto)
            }
        }

        if (!settings.bodiesOnlyForInlines || function.isInline || (settings.publicAbiOnly && isInsideInline)) {
            function.body?.let { proto.body = serializeIrStatementBody(it) }
        }
        isInsideInline = isInsideInlineBefore

        return proto.build()
    }

    private fun serializeIrConstructor(declaration: IrConstructor): ProtoConstructor =
        ProtoConstructor.newBuilder()
            .setBase(serializeIrFunctionBase(declaration, FunctionFlags.encode(declaration)))
            .build()

    private fun serializeIrFunction(declaration: IrSimpleFunction): ProtoFunction {
        declaration.erasedTopLevelCopy?.let { preprocessedToOriginalInlineFunctions[it] = declaration }

        val proto = ProtoFunction.newBuilder()
            .setBase(serializeIrFunctionBase(declaration, FunctionFlags.encode(declaration)))

        declaration.overriddenSymbols.forEach {
            proto.addOverridden(serializeIrSymbol(it))
        }

        return proto.build()
    }

    private fun serializeIrAnonymousInit(declaration: IrAnonymousInitializer): ProtoAnonymousInit {
        val proto = ProtoAnonymousInit.newBuilder()
            .setBase(serializeIrDeclarationBase(declaration, null))

        proto.body = serializeIrStatementBody(declaration.body)

        return proto.build()
    }

    private fun serializeIrLocalDelegatedProperty(variable: IrLocalDelegatedProperty): ProtoLocalDelegatedProperty {
        val proto = ProtoLocalDelegatedProperty.newBuilder()
            .setBase(serializeIrDeclarationBase(variable, LocalVariableFlags.encode(variable)))
            .setNameType(serializeNameAndType(variable.name, variable.type))

        proto.delegate = serializeIrVariable(variable.delegate)
        proto.getter = serializeIrFunction(variable.getter)
        variable.setter?.let { proto.setSetter(serializeIrFunction(it)) }

        return proto.build()
    }

    private fun serializeIrProperty(property: IrProperty): ProtoProperty {
        val proto = ProtoProperty.newBuilder()
            .setBase(serializeIrDeclarationBase(property, PropertyFlags.encode(property)))
            .setName(serializeName(property.name))

        property.backingField?.takeUnless { skipIfPrivate(it) }?.let { proto.backingField = serializeIrField(it) }
        property.getter?.takeUnless { skipIfPrivate(it) }?.let { proto.getter = serializeIrFunction(it) }
        property.setter?.takeUnless { skipIfPrivate(it) }?.let { proto.setter = serializeIrFunction(it) }

        return proto.build()
    }

    private fun serializeIrField(field: IrField): ProtoField {
        val proto = ProtoField.newBuilder()
            .setBase(serializeIrDeclarationBase(field, FieldFlags.encode(field)))
            .setNameType(serializeNameAndType(field.name, field.type))
        if (!(settings.bodiesOnlyForInlines &&
                    (field.parent as? IrDeclarationWithVisibility)?.visibility != DescriptorVisibilities.LOCAL &&
                    (field.initializer?.expression !is IrConst))
        ) {
            val initializer = field.initializer?.expression
            if (field.correspondingPropertySymbol?.owner?.isConst == true)
                require(initializer is IrConst) {
                    "This is a compiler bug, please report it to https://kotl.in/issue : const val property must have a const initializer:\n${field.render()}"
                }

            if (initializer != null) {
                proto.initializer = serializeIrExpressionBody(initializer)
            }
        }
        return proto.build()
    }

    private fun serializeIrVariable(variable: IrVariable): ProtoVariable {
        val proto = ProtoVariable.newBuilder()
            .setBase(serializeIrDeclarationBase(variable, LocalVariableFlags.encode(variable)))
            .setNameType(serializeNameAndType(variable.name, variable.type))
        variable.initializer?.let { proto.initializer = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeIrClass(clazz: IrClass): ProtoClass {
        val proto = ProtoClass.newBuilder()
            .setBase(serializeIrDeclarationBase(clazz, ClassFlags.encode(clazz, settings.languageVersionSettings)))
            .setName(serializeName(clazz.name))


        when (val representation = clazz.valueClassRepresentation) {
            is MultiFieldValueClassRepresentation ->
                proto.multiFieldValueClassRepresentation = serializeMultiFieldValueClassRepresentation(representation)
            is InlineClassRepresentation -> proto.inlineClassRepresentation = serializeInlineClassRepresentation(representation)
            null -> Unit
        }

        clazz.declarations.forEach {
            if (memberNeedsSerialization(it)) proto.addDeclaration(serializeDeclaration(it))
        }

        clazz.typeParameters.forEach {
            proto.addTypeParameter(serializeIrTypeParameter(it))
        }

        clazz.thisReceiver?.let { proto.thisReceiver = serializeIrValueParameter(it) }

        clazz.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }

        clazz.sealedSubclasses.forEach {
            proto.addSealedSubclass(serializeIrSymbol(it))
        }

        return proto.build()
    }

    private fun serializeInlineClassRepresentation(representation: InlineClassRepresentation<IrSimpleType>): ProtoIrInlineClassRepresentation =
        ProtoIrInlineClassRepresentation.newBuilder().apply {
            underlyingPropertyName = serializeName(representation.underlyingPropertyName)
            // TODO: consider not writing type if the property is public, similarly to metadata
            underlyingPropertyType = serializeIrType(representation.underlyingType)
        }.build()

    private fun serializeMultiFieldValueClassRepresentation(representation: MultiFieldValueClassRepresentation<IrSimpleType>): ProtoIrMultiFieldValueClassRepresentation =
        ProtoIrMultiFieldValueClassRepresentation.newBuilder().apply {
            addAllUnderlyingPropertyName(representation.underlyingPropertyNamesToTypes.map { (name, _) -> serializeName(name) })
            addAllUnderlyingPropertyType(representation.underlyingPropertyNamesToTypes.map { (_, irType) -> serializeIrType(irType) })
        }.build()

    private fun serializeIrTypeAlias(typeAlias: IrTypeAlias): ProtoTypeAlias {
        val proto = ProtoTypeAlias.newBuilder()

        proto.setBase(serializeIrDeclarationBase(typeAlias, TypeAliasFlags.encode(typeAlias))).nameType =
            serializeNameAndType(typeAlias.name, typeAlias.expandedType)

        typeAlias.typeParameters.forEach {
            proto.addTypeParameter(serializeIrTypeParameter(it))
        }

        return proto.build()
    }

    private fun serializeIrEnumEntry(enumEntry: IrEnumEntry): ProtoEnumEntry {
        val proto = ProtoEnumEntry.newBuilder()
            .setBase(serializeIrDeclarationBase(enumEntry, null))
            .setName(serializeName(enumEntry.name))

        enumEntry.initializerExpression?.let {
            proto.initializer = serializeIrExpressionBody(it.expression)
        }
        enumEntry.correspondingClass?.let {
            proto.correspondingClass = serializeIrClass(it)
        }
        return proto.build()
    }

    fun serializeDeclaration(declaration: IrDeclaration): ProtoDeclaration {
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

    // This class is needed solely to have generated `equals()` and `hashCode()` for `FileEntry`, to compare objects by value.
    // For correct deduplication, it must have the same fields as `FileEntry` in `KotlinIr.proto`.
    // TODO: KT-74258: bump Protobuf version to >3.x to have generated `ProtoFileEntry.equals()` and `ProtoFileEntry.hashCode()`
    data class ProtoFileEntryDeduplicationKey(
        val name: String,
        val lineStartOffsetList: List<Int>,
    )

    private fun serializeFileEntryId(entry: IrFileEntry, includeLineStartOffsets: Boolean = true): Int {
        val proto = serializeFileEntry(entry, includeLineStartOffsets)
        return protoIrFileEntryMap.getOrPut(ProtoFileEntryDeduplicationKey(proto.name, proto.lineStartOffsetList)) {
            protoIrFileEntryArray.add(proto)
            protoIrFileEntryArray.size - 1
        }
    }

    private fun serializeFileEntry(entry: IrFileEntry, includeLineStartOffsets: Boolean = true): ProtoFileEntry =
        ProtoFileEntry.newBuilder()
            .setName(entry.matchAndNormalizeFilePath())
            .applyIf(includeLineStartOffsets) { addAllLineStartOffset(entry.lineStartOffsetsForSerialization) }
            .build()

    open fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean = false
    open fun backendSpecificExplicitRootExclusion(node: IrAnnotationContainer): Boolean = false
    open fun keepOrderOfProperties(property: IrProperty): Boolean = !property.isConst
    open fun backendSpecificSerializeAllMembers(irClass: IrClass) = false
    open fun backendSpecificMetadata(irFile: IrFile): FileBackendSpecificMetadata? = null

    private fun skipIfPrivate(declaration: IrDeclaration) =
        settings.publicAbiOnly
                && (declaration as? IrDeclarationWithVisibility)?.let { !it.visibility.isPublicAPI && it.visibility != INTERNAL } == true
                // Always keep private interfaces and type aliases as they can be part of public type hierarchies.
                && (declaration as? IrClass)?.isInterface != true && declaration !is IrTypeAlias

    open fun memberNeedsSerialization(member: IrDeclaration): Boolean {
        val parent = member.parent
        require(parent is IrClass)
        if (backendSpecificSerializeAllMembers(parent)) return true
        if (settings.bodiesOnlyForInlines && member is IrAnonymousInitializer && parent.visibility != DescriptorVisibilities.LOCAL)
            return false
        if (skipIfPrivate(member)) {
            return false
        }

        return (!member.isFakeOverride)
    }

    private fun fillPlatformExplicitlyExported(file: IrFile, proto: ProtoFile.Builder) {

        if (backendSpecificExplicitRoot(file)) {
            for (declaration in file.declarations) {
                if (backendSpecificExplicitRootExclusion(declaration)) continue
                proto.addExplicitlyExportedToCompiler(serializeIrSymbol(declaration.symbol))
            }
        } else {
            file.acceptVoid(
                object : IrVisitorVoid() {
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

                    override fun visitProperty(declaration: IrProperty) {
                        if (backendSpecificExplicitRoot(declaration)) {
                            proto.addExplicitlyExportedToCompiler(serializeIrSymbol(declaration.symbol))
                        }
                        super.visitDeclaration(declaration)
                    }
                }
            )
        }
    }

    fun <T> inFile(file: IrFile, block: () -> T): T = declarationTable.inFile(file, block)

    fun serializeIrFile(file: IrFile): SerializedIrFile = inFile(file) {
        val topLevelDeclarations = mutableListOf<SerializedDeclaration>()

        val proto = ProtoFile.newBuilder()
            .addAllFqName(serializeFqName(file.packageFqName.asString()))
            .addAllAnnotation(serializeAnnotations(file.annotations))

        file.declarations.forEach {
            if (skipIfPrivate(it)) {
                // Skip the declaration if producing header klib and the declaration is not public.
                return@forEach
            }
            if (it.descriptor.isExpectMember && !it.descriptor.isSerializableExpectClass) {
                // Skip the declaration unless it is `expect annotation class` marked with `OptionalExpectation`
                // without the corresponding `actual` counterpart for the current leaf target.
                return@forEach
            }

            val serializedDeclaration = serializeTopLevelDeclaration(it)
            topLevelDeclarations.add(serializedDeclaration)
            proto.addDeclarationId(serializedDeclaration.id)
        }

        val preprocessedInlineFunctions =
            preprocessedToOriginalInlineFunctions.map { (preprocessedInlineFunction, originalInlineFunction) ->
                val originalIdSignature = declarationTable.signatureByDeclaration(
                    originalInlineFunction,
                    compatibleMode = false,
                    recordInSignatureClashDetector = false
                )
                val originalSigIndex = protoIdSignatureMap[originalIdSignature]
                    ?: error("Not found ID for $originalIdSignature (${originalInlineFunction.render()})")
                proto.addPreprocessedInlineFunctions(originalSigIndex)

                val serializedPreprocessedInlineFunction = serializeTopLevelDeclaration(preprocessedInlineFunction)
                SerializedDeclaration(originalSigIndex, serializedPreprocessedInlineFunction.bytes)
            }

        val includeLineStartOffsets = !settings.publicAbiOnly || fileContainsInline
        if (settings.abiCompatibilityLevel.isAtLeast(ABI_LEVEL_2_2)) {
            // KLIBs with ABI version >= 2.2.0 have `fileEntries.knf` file with `file entries` table.
            proto.setFileEntryId(serializeFileEntryId(file.fileEntry, includeLineStartOffsets = includeLineStartOffsets))
        } else {
            proto.setFileEntry(serializeFileEntry(file.fileEntry, includeLineStartOffsets = includeLineStartOffsets))
        }

        // TODO: is it Konan specific?

        // Make sure that all top level properties are initialized on library's load.
        file.declarations
            .filterIsInstanceAnd<IrProperty> { it.backingField?.initializer != null && keepOrderOfProperties(it) && !skipIfPrivate(it) }
            .forEach {
                val fieldSymbol = it.backingField?.symbol ?: error("Not found ID ${it.render()}")
                proto.addExplicitlyExportedToCompiler(serializeIrSymbol(fieldSymbol))
            }

        fillPlatformExplicitlyExported(file, proto)

        SerializedIrFile(
            fileData = proto.build().toByteArray(),
            fqName = file.packageFqName.asString(),
            path = file.path,
            types = IrArrayWriter(protoTypeArray.byteArrays).writeIntoMemory(),
            signatures = IrArrayWriter(protoIdSignatureArray.map { it.toByteArray() }).writeIntoMemory(),
            strings = IrStringWriter(protoStringArray).writeIntoMemory(),
            bodies = IrArrayWriter(protoBodyArray.map { it.toByteArray() }).writeIntoMemory(),
            declarations = IrDeclarationWriter(topLevelDeclarations).writeIntoMemory(),
            inlineDeclarations = IrDeclarationWriter(preprocessedInlineFunctions).writeIntoMemory(),
            debugInfo = IrStringWriter(protoDebugInfoArray).writeIntoMemory(),
            backendSpecificMetadata = backendSpecificMetadata(file)?.toByteArray(),
            fileEntries = with(protoIrFileEntryArray) {
                if (isNotEmpty()) {
                    requireAbiAtLeast(ABI_LEVEL_2_2, { "IR file entries table" }) { file }
                    IrArrayWriter(protoIrFileEntryArray.map { it.toByteArray() }).writeIntoMemory()
                } else {
                    null
                }
            },
        )
    }

    private fun serializeTopLevelDeclaration(topLevelDeclaration: IrDeclaration): SerializedDeclaration {
        val byteArray = serializeDeclaration(topLevelDeclaration).toByteArray()
        val idSig = declarationTable.signatureByDeclaration(
            topLevelDeclaration,
            compatibleMode = false,
            recordInSignatureClashDetector = false
        )
        require(idSig == idSig.topLevelSignature()) { "IdSig: $idSig\ntopLevel: ${idSig.topLevelSignature()}" }
        require(!idSig.isPackageSignature()) { "IsSig: $idSig\nDeclaration: ${topLevelDeclaration.render()}" }

        // TODO: keep order similar
        val sigIndex = protoIdSignatureMap[idSig] ?: error("Not found ID for $idSig (${topLevelDeclaration.render()})")
        return SerializedDeclaration(sigIndex, byteArray)
    }

    private fun tryMatchPath(fileName: String): String? {
        val file = File(fileName)
        val path = file.toPath()

        for (base in settings.sourceBaseDirs) {
            if (path.startsWith(base)) {
                return file.toRelativeString(File(base))
            }
        }

        return null
    }

    private fun IrFileEntry.matchAndNormalizeFilePath(): String {
        tryMatchPath(name)?.let {
            return it.replace(File.separatorChar, '/')
        }

        if (!settings.normalizeAbsolutePaths) return name

        return name.replace(File.separatorChar, '/')

    }

    private inline fun <T : IrElement> requireAbiAtLeast(
        @Suppress("SameParameterValue") abiCompatibilityLevel: KlibAbiCompatibilityLevel,
        prefix: (T) -> String = { it::class.simpleName ?: "IrElement" },
        irNode: () -> T,
    ) {
        if (!settings.abiCompatibilityLevel.isAtLeast(abiCompatibilityLevel))
            serializationNotSupportedAtCurrentAbiLevel(prefix, irNode)
    }

    private inline fun <T : IrElement> serializationNotSupportedAtCurrentAbiLevel(
        prefix: (T) -> String = { it::class.simpleName ?: "IrElement" },
        irNode: () -> T,
    ): Nothing {
        val irNode = irNode()
        error("${prefix(irNode)} serialization is not supported at ABI compatibility level ${settings.abiCompatibilityLevel}: ${irNode.render()}")
    }
}

internal fun IrElement.isValidConstantAnnotationArgument(): Boolean =
    this is IrConst || this is IrGetEnumValue || this is IrClassReference ||
            (this is IrVararg && elements.all { it.isValidConstantAnnotationArgument() }) ||
            (this is IrConstructorCall && arguments.all { it?.isValidConstantAnnotationArgument() ?: true })
