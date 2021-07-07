/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.SerializedDeclaration
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SkippedDeclaration
import org.jetbrains.kotlin.library.TopLevelDeclaration
import org.jetbrains.kotlin.library.impl.IrMemoryArrayWriter
import org.jetbrains.kotlin.library.impl.IrMemoryDeclarationWriter
import org.jetbrains.kotlin.library.impl.IrMemoryStringWriter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual as ProtoActual
import org.jetbrains.kotlin.backend.common.serialization.proto.FieldAccessCommon as ProtoFieldAccessCommon
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.ScopeLocalIdSignature as ProtoScopeLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature as ProtoFileSignature
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
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorDeclaration as ProtoErrorDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorExpression as ProtoErrorExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorType as ProtoErrorType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrField as ProtoField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionExpression as ProtoFunctionExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionReference as ProtoFunctionReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrRawFunctionReference as ProtoRawFunctionReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturnableBlock as ProtoReturnableBlock
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetClass as ProtoGetClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetEnumValue as ProtoGetEnumValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetField as ProtoGetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetObject as ProtoGetObject
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetValue as ProtoGetValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInlineClassRepresentation as ProtoIrInlineClassRepresentation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInstanceInitializerCall as ProtoInstanceInitializerCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedProperty as ProtoLocalDelegatedProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedPropertyReference as ProtoLocalDelegatedPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrOperation as ProtoOperation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrPropertyReference as ProtoPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturn as ProtoReturn
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturnableBlockReturn as ProtoReturnableBlockReturn
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
import org.jetbrains.kotlin.backend.common.serialization.proto.PublicIdSignature as ProtoPublicIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LoweredIdSignature as ProtoLoweredIdSignature

open class IrFileSerializer(
    val messageLogger: IrMessageLogger,
    private val declarationTable: DeclarationTable,
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    private val bodiesOnlyForInlines: Boolean = false,
    private val skipExpects: Boolean = false,
    // required for JS IC caches
    private val skipMutableState: Boolean = false,
    private val allowNullTypes: Boolean = false,
    private val allowErrorStatementOrigins: Boolean = false, // TODO: support InlinerExpressionLocationHint
) {
    private val loopIndex = mutableMapOf<IrLoop, Int>()
    private var currentLoopIndex = 0

    // For every actual we keep a corresponding expects' uniqIds.
    // The linker substitutes actual symbols when asked for an expect uniqId.
    private val expectActualTable = ExpectActualTable(expectDescriptorToSymbol)

    // The same type can be used multiple times in a file
    // so use this index to store type data only once.
    private val protoTypeMap = mutableMapOf<IrTypeKey, Int>()
    private val protoTypeArray = arrayListOf<ProtoType>()

    private val protoStringMap = mutableMapOf<String, Int>()
    private val protoStringArray = arrayListOf<String>()

    // The same signature could be used multiple times in a file
    // so use this index to store signature only once.
    private val protoIdSignatureMap = mutableMapOf<IdSignature, Int>()
    private val protoIdSignatureArray = arrayListOf<ProtoIdSignature>()

    private val protoBodyArray = mutableListOf<XStatementOrExpression>()

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

    fun serializeIrExpressionBody(expression: IrExpression): Int {
        protoBodyArray.add(XStatementOrExpression.XExpression(serializeExpression(expression)))
        return protoBodyArray.size - 1
    }

    fun serializeIrStatementBody(statement: IrElement): Int {
        protoBodyArray.add(XStatementOrExpression.XStatement(serializeStatement(statement)))
        return protoBodyArray.size - 1
    }

    /* ------- Common fields ---------------------------------------------------- */

    fun serializeIrDeclarationOrigin(origin: IrDeclarationOrigin): Int = serializeString((origin as IrDeclarationOriginImpl).name)

    private fun serializeIrStatementOrigin(origin: IrStatementOrigin): Int =
        serializeString((origin as? IrStatementOriginImpl)?.debugName ?: if (allowErrorStatementOrigins) "error_origin" else error("Unable to serialize origin ${origin.javaClass.name}"))

    private fun serializeCoordinates(start: Int, end: Int): Long = BinaryCoordinates.encode(start, end)

    /* ------- Strings ---------------------------------------------------------- */

    private fun serializeString(value: String): Int = protoStringMap.getOrPut(value) {
        protoStringArray.add(value)
        protoStringArray.size - 1
    }

    private fun serializeName(name: Name): Int = serializeString(name.toString())

    /* ------- IdSignature ------------------------------------------------------ */

    private fun serializePublicSignature(signature: IdSignature.PublicSignature): ProtoPublicIdSignature {
        val proto = ProtoPublicIdSignature.newBuilder()
        proto.addAllPackageFqName(serializeFqName(signature.packageFqName))
        proto.addAllDeclarationFqName(serializeFqName(signature.declarationFqName))

        signature.id?.let { proto.memberUniqId = it }
        if (signature.mask != 0L) {
            proto.flags = signature.mask
        }

        return proto.build()
    }

    private fun serializeAccessorSignature(signature: IdSignature.AccessorSignature): ProtoAccessorIdSignature {
        val proto = ProtoAccessorIdSignature.newBuilder()

        proto.propertySignature = protoIdSignature(signature.propertySignature)
        with(signature.accessorSignature) {
            proto.name = serializeString(shortName)
            proto.accessorHashId = id ?: error("Expected hash Id")
            if (mask != 0L) {
                proto.flags = mask
            }
        }

        return proto.build()
    }

    private fun serializePrivateSignature(signature: IdSignature.FileLocalSignature): ProtoFileLocalIdSignature {
        val proto = ProtoFileLocalIdSignature.newBuilder()

        proto.container = protoIdSignature(signature.container)
        proto.localId = signature.id

        return proto.build()
    }

    private fun serializeScopeLocalSignature(signature: IdSignature.ScopeLocalDeclaration): Int = signature.id

    private fun serializePrivateSignature(signature: IdSignature.GlobalFileLocalSignature): ProtoFileLocalIdSignature {
        val proto = ProtoFileLocalIdSignature.newBuilder()

        proto.container = protoIdSignature(signature.container)
        proto.localId = signature.id
        proto.file = serializeString(signature.filePath)

        return proto.build()
    }

    private fun serializeScopeLocalSignature(signature: IdSignature.GlobalScopeLocalDeclaration): ProtoScopeLocalIdSignature {
        val proto = ProtoScopeLocalIdSignature.newBuilder()

        proto.id = signature.id
        proto.file = serializeString(signature.filePath)

        return proto.build()
    }

    private fun serializeFileSignature(): ProtoFileSignature {
        return ProtoFileSignature.newBuilder().build()
    }

    private fun serializeLoweredDeclarationSignature(signature: IdSignature.LoweredDeclarationSignature): ProtoLoweredIdSignature {
        val proto = ProtoLoweredIdSignature.newBuilder()

        proto.parentSignature = protoIdSignature(signature.original)
        proto.stage = signature.stage
        proto.index = signature.index

        return proto.build()
    }

    fun serializeIdSignature(idSignature: IdSignature): ProtoIdSignature {
        val proto = ProtoIdSignature.newBuilder()
        when (idSignature) {
            is IdSignature.PublicSignature -> proto.publicSig = serializePublicSignature(idSignature)
            is IdSignature.AccessorSignature -> proto.accessorSig = serializeAccessorSignature(idSignature)
            is IdSignature.FileLocalSignature -> proto.privateSig = serializePrivateSignature(idSignature)
            is IdSignature.ScopeLocalDeclaration -> proto.scopedLocalSig = serializeScopeLocalSignature(idSignature)
            is IdSignature.LoweredDeclarationSignature -> proto.icSig = serializeLoweredDeclarationSignature(idSignature)
            is IdSignature.FileSignature -> proto.fileSig = serializeFileSignature()
            is IdSignature.GlobalFileLocalSignature -> proto.privateSig = serializePrivateSignature(idSignature)
            is IdSignature.GlobalScopeLocalDeclaration -> proto.externalScopedLocalSig = serializeScopeLocalSignature(idSignature)
        }
        return proto.build()
    }

    private fun protoIdSignature(declaration: IrDeclaration): Int {
        val idSig = declarationTable.signatureByDeclaration(declaration)
        return protoIdSignature(idSig)
    }

    fun protoIdSignature(idSig: IdSignature): Int {
        return protoIdSignatureMap.getOrPut(idSig) {
            protoIdSignatureArray.add(serializeIdSignature(idSig))
            protoIdSignatureArray.size - 1
        }
    }

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
                if (symbol.owner.correspondingPropertySymbol?.owner.let { it == null || it.isDelegated })
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

    fun serializeIrSymbol(symbol: IrSymbol): Long {
        val symbolKind = protoSymbolKind(symbol)

        val signatureId = when {
            symbol is IrFileSymbol -> protoIdSignature(IdSignature.FileSignature(symbol)) // TODO: special signature for files?
            else -> {
                val declaration = symbol.owner as? IrDeclaration ?: error("Expected IrDeclaration: ${symbol.owner.render()}")
                protoIdSignature(declaration)
            }
        }

        return BinarySymbolData.encode(symbolKind, signatureId)
    }

    /* ------- IrTypes ---------------------------------------------------------- */

    private fun serializeAnnotations(annotations: List<IrConstructorCall>) =
        annotations.map { serializeConstructorCall(it) }

    private fun serializeFqName(fqName: String): List<Int> = fqName.split(".").map(::serializeString)

    private fun serializeIrStarProjection() = BinaryTypeProjection.STAR_CODE

    private fun serializeIrTypeProjection(argument: IrTypeProjection) =
        BinaryTypeProjection
            .encodeType(argument.variance, serializeIrType(argument.type))

    private fun serializeTypeArgument(argument: IrTypeArgument): Long {
        return when (argument) {
            is IrStarProjection -> serializeIrStarProjection()
            is IrTypeProjection -> serializeIrTypeProjection(argument)
            else -> TODO("Unexpected type argument kind: $argument")
        }
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
        val annotations: List<IrConstructorCall>,
        val abbreviation: IrTypeAbbreviation?
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
            annotations = this.annotations,
            abbreviation = (this as? IrSimpleType)?.abbreviation
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

    fun serializeIrType(type: IrType?) = if (type == null) -1 else protoTypeMap.getOrPut(type.toIrTypeKey) {
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

    private val returnableBlockStack = ArrayDeque<IrBlock>()

    private fun <T> withBlock(block: IrReturnableBlock, fn: () -> T): T {
        returnableBlockStack.addLast(block)
        try {
            return fn()
        } finally {
            returnableBlockStack.removeLast()
        }
    }

    private fun serializeReturnableBlock(returnableBlock: IrReturnableBlock): ProtoReturnableBlock = withBlock(returnableBlock) {
        val proto = ProtoReturnableBlock.newBuilder()

        returnableBlock.origin?.let { proto.setOriginName(serializeIrStatementOrigin(it)) }

        returnableBlock.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }

        returnableBlock.inlineFunctionSymbol?.let {
            proto.inlineFunctionSymbol = serializeIrSymbol(it)
        }

        proto.build()
    }

    private fun serializeBlock(block: IrBlock): ProtoBlock {
        val proto = ProtoBlock.newBuilder()

        block.origin?.let { proto.setOriginName(serializeIrStatementOrigin(it)) }

        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeComposite(composite: IrComposite): ProtoComposite {
        val proto = ProtoComposite.newBuilder()

        composite.origin?.let { proto.setOriginName(serializeIrStatementOrigin(it)) }
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
        val proto = ProtoMemberAccessCommon.newBuilder()
        if (call.extensionReceiver != null) {
            proto.extensionReceiver = serializeExpression(call.extensionReceiver!!)
        }

        if (call.dispatchReceiver != null) {
            proto.dispatchReceiver = serializeExpression(call.dispatchReceiver!!)
        }

        for (index in 0 until call.typeArgumentsCount) {
            val typeArgument = call.getTypeArgument(index)
            if (typeArgument == null && !allowNullTypes) error("Expected type argument at $index in ${call.render()}")
            proto.addTypeArgument(serializeIrType(typeArgument))
        }

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
        call.origin?.let { proto.originName = serializeIrStatementOrigin(it) }

        call.superQualifierSymbol?.let {
            proto.`super` = serializeIrSymbol(it)
        }
        proto.memberAccess = serializeMemberAccessCommon(call)

        return proto.build()
    }

    fun serializeConstructorCall(call: IrConstructorCall): ProtoConstructorCall =
        ProtoConstructorCall.newBuilder().apply {
            symbol = serializeIrSymbol(call.symbol)
            constructorTypeArgumentsCount = call.constructorTypeArgumentsCount
            memberAccess = serializeMemberAccessCommon(call)
            call.origin?.let {
                originName = serializeIrStatementOrigin(it)
            }
        }.build()

    private fun serializeFunctionExpression(functionExpression: IrFunctionExpression): ProtoFunctionExpression =
        ProtoFunctionExpression.newBuilder().apply {
            function = serializeIrFunction(functionExpression.function)
            originName = serializeIrStatementOrigin(functionExpression.origin)
        }.build()

    private fun serializeFunctionReference(callable: IrFunctionReference): ProtoFunctionReference {
        val proto = ProtoFunctionReference.newBuilder()
            .setSymbol(serializeIrSymbol(callable.symbol))
            .setMemberAccess(serializeMemberAccessCommon(callable))

        callable.reflectionTarget?.let { proto.reflectionTargetSymbol = serializeIrSymbol(it) }
        callable.origin?.let { proto.originName = serializeIrStatementOrigin(it) }
        return proto.build()
    }

    private fun serializeRawFunctionReference(rawReference: IrRawFunctionReference): ProtoRawFunctionReference {
        val proto = ProtoRawFunctionReference.newBuilder()
            .setSymbol(serializeIrSymbol(rawReference.symbol))

        return proto.build()
    }

    private fun serializeIrLocalDelegatedPropertyReference(
        callable: IrLocalDelegatedPropertyReference
    ): ProtoLocalDelegatedPropertyReference {
        val proto = ProtoLocalDelegatedPropertyReference.newBuilder()
            .setDelegate(serializeIrSymbol(callable.delegate))
            .setGetter(serializeIrSymbol(callable.getter))
            .setSymbol(serializeIrSymbol(callable.symbol))

        callable.origin?.let { proto.setOriginName(serializeIrStatementOrigin(it)) }
        callable.setter?.let { proto.setSetter(serializeIrSymbol(it)) }

        return proto.build()
    }

    private fun serializePropertyReference(callable: IrPropertyReference): ProtoPropertyReference {
        val proto = ProtoPropertyReference.newBuilder()
            .setMemberAccess(serializeMemberAccessCommon(callable))
            .setSymbol(serializeIrSymbol(callable.symbol))

        callable.origin?.let { proto.originName = serializeIrStatementOrigin(it) }
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
                expression.origin?.let { originName = serializeIrStatementOrigin(it) }
            }
            .build()

    private fun serializeGetValue(expression: IrGetValue): ProtoGetValue =
        ProtoGetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol)).apply {
                expression.origin?.let { originName = serializeIrStatementOrigin(it) }
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
        if (expression.returnTargetSymbol is IrReturnableBlockSymbol) {
            val targetBlock = expression.returnTargetSymbol.owner

            var t = returnableBlockStack.size - 1
            while (t >= 0 && returnableBlockStack[t] != targetBlock) --t

            val proto = ProtoReturnableBlockReturn.newBuilder()
                .setUpCnt(returnableBlockStack.size - t)
                .setValue(serializeExpression(expression.value))
            operation.returnableBlockReturn = proto.build()
        } else {
            val proto = ProtoReturn.newBuilder()
                .setReturnTarget(serializeIrSymbol(expression.returnTargetSymbol))
                .setValue(serializeExpression(expression.value))
            operation.`return` = proto.build()
        }
    }

    private fun serializeSetField(expression: IrSetField): ProtoSetField =
        ProtoSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value)).apply {
                expression.origin?.let { originName = serializeIrStatementOrigin(it) }
            }
            .build()

    private fun serializeSetValue(expression: IrSetValue): ProtoSetValue =
        ProtoSetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
            .setValue(serializeExpression(expression.value)).apply {
                expression.origin?.let { originName = serializeIrStatementOrigin(it) }
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

        expression.origin?.let { proto.setOriginName(serializeIrStatementOrigin(it)) }

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
                expression.origin?.let { setOriginName(serializeIrStatementOrigin(it)) }
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
            is IrRawFunctionReference -> operationProto.rawFunctionReference = serializeRawFunctionReference(expression)
            else -> {
                TODO("Expression serialization not implemented yet: ${expression.render()}.")
            }
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
        if (!skipExpects) expectActualTable.findExpectsForActuals(declaration)
        return with(ProtoDeclarationBase.newBuilder()) {
            symbol = serializeIrSymbol((declaration as IrSymbolOwner).symbol)
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
        val proto = ProtoValueParameter.newBuilder()
            .setBase(serializeIrDeclarationBase(parameter, ValueParameterFlags.encode(parameter)))
            .setNameType(serializeNameAndType(parameter.name, parameter.type))

        if (!skipMutableState) {
            parameter.varargElementType?.let { proto.setVarargElementType(serializeIrType(it)) }
            parameter.defaultValue?.let { proto.setDefaultValue(serializeIrExpressionBody(it.expression)) }
        } else {
            proto.index = parameter.index
        }

        return proto.build()
    }

    private fun serializeIrTypeParameter(parameter: IrTypeParameter): ProtoTypeParameter {
        val proto = ProtoTypeParameter.newBuilder()
            .setBase(serializeIrDeclarationBase(parameter, TypeParameterFlags.encode(parameter)))
            .setName(serializeName(parameter.name))
        parameter.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }

        if (skipMutableState) {
            proto.index = parameter.index
            proto.isGlobal = true // parameter.parent is IrClass
        }

        return proto.build()
    }

    private fun serializeIrFunctionBase(function: IrFunction, flags: Long): ProtoFunctionBase {
        val proto = ProtoFunctionBase.newBuilder()
            .setBase(serializeIrDeclarationBase(function, flags))
            .setNameType(serializeNameAndType(function.name, function.returnType))

        if (!skipMutableState) {
            function.typeParameters.forEach {
                proto.addTypeParameter(serializeIrTypeParameter(it))
            }
            function.dispatchReceiverParameter?.let { proto.setDispatchReceiver(serializeIrValueParameter(it)) }
            function.extensionReceiverParameter?.let { proto.setExtensionReceiver(serializeIrValueParameter(it)) }
            function.valueParameters.forEach {
                proto.addValueParameter(serializeIrValueParameter(it))
            }

            if (!bodiesOnlyForInlines || function.isInline) {
                function.body?.let { proto.body = serializeIrStatementBody(it) }
            }
        }

        return proto.build()
    }

    private fun serializeIrConstructor(declaration: IrConstructor): ProtoConstructor =
        ProtoConstructor.newBuilder()
            .setBase(serializeIrFunctionBase(declaration, FunctionFlags.encode(declaration)))
            .build()

    private fun serializeIrFunction(declaration: IrSimpleFunction): ProtoFunction {
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

        if (!skipMutableState) {
            proto.setBody(serializeIrStatementBody(declaration.body))
        }

        return proto.build()
    }

    private fun serializeIrLocalDelegatedProperty(variable: IrLocalDelegatedProperty): ProtoLocalDelegatedProperty {
        val proto = ProtoLocalDelegatedProperty.newBuilder()
            .setBase(serializeIrDeclarationBase(variable, LocalVariableFlags.encode(variable)))
            .setNameType(serializeNameAndType(variable.name, variable.type))

        if (!skipMutableState) {
            proto.delegate = serializeIrVariable(variable.delegate)
            proto.getter = serializeIrFunction(variable.getter)
            variable.setter?.let { proto.setSetter(serializeIrFunction(it)) }
        }

        return proto.build()
    }

    private fun serializeIrProperty(property: IrProperty): ProtoProperty {
        val proto = ProtoProperty.newBuilder()
            .setBase(serializeIrDeclarationBase(property, PropertyFlags.encode(property)))
            .setName(serializeName(property.name))

        if (!skipMutableState) {
            property.backingField?.let { proto.backingField = serializeIrField(it) }
            property.getter?.let { proto.getter = serializeIrFunction(it) }
            property.setter?.let { proto.setter = serializeIrFunction(it) }
        }

        return proto.build()
    }

    private fun serializeIrField(field: IrField): ProtoField {
        val proto = ProtoField.newBuilder()
            .setBase(serializeIrDeclarationBase(field, FieldFlags.encode(field)))
            .setNameType(serializeNameAndType(field.name, field.type))
        if (!skipMutableState) {
            val initializer = field.initializer?.expression
            if (initializer != null) {
                proto.initializer = serializeIrExpressionBody(initializer)
            }
        }
        return proto.build()
    }

    fun serializeIrVariable(variable: IrVariable): ProtoVariable {
        val proto = ProtoVariable.newBuilder()
            .setBase(serializeIrDeclarationBase(variable, LocalVariableFlags.encode(variable)))
            .setNameType(serializeNameAndType(variable.name, variable.type))
        variable.initializer?.let { proto.initializer = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeIrClass(clazz: IrClass): ProtoClass {
        val proto = ProtoClass.newBuilder()
            .setBase(serializeIrDeclarationBase(clazz, ClassFlags.encode(clazz)))
            .setName(serializeName(clazz.name))

        val representation = clazz.inlineClassRepresentation
        if (representation != null) {
            proto.inlineClassRepresentation = serializeInlineClassRepresentation(representation)
        }

        if (!skipMutableState) {
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
        }

        return proto.build()
    }

    fun serializeInlineClassRepresentation(representation: InlineClassRepresentation<IrSimpleType>): ProtoIrInlineClassRepresentation =
        ProtoIrInlineClassRepresentation.newBuilder().apply {
            underlyingPropertyName = serializeName(representation.underlyingPropertyName)
            // TODO: consider not writing type if the property is public, similarly to metadata
            underlyingPropertyType = serializeIrType(representation.underlyingType)
        }.build()

    private fun serializeIrTypeAlias(typeAlias: IrTypeAlias): ProtoTypeAlias {
        val proto = ProtoTypeAlias.newBuilder()

        proto.setBase(serializeIrDeclarationBase(typeAlias, TypeAliasFlags.encode(typeAlias)))
            .setNameType(serializeNameAndType(typeAlias.name, typeAlias.expandedType))

        typeAlias.typeParameters.forEach {
            if (!skipMutableState) {
                proto.addTypeParameter(serializeIrTypeParameter(it))
            }
        }

        return proto.build()
    }

    private fun serializeIrErrorDeclaration(errorDeclaration: IrErrorDeclaration): ProtoErrorDeclaration {
        val proto = ProtoErrorDeclaration.newBuilder()
            .setCoordinates(serializeCoordinates(errorDeclaration.startOffset, errorDeclaration.endOffset))
        return proto.build()
    }

    private fun serializeIrEnumEntry(enumEntry: IrEnumEntry): ProtoEnumEntry {
        val proto = ProtoEnumEntry.newBuilder()
            .setBase(serializeIrDeclarationBase(enumEntry, null))
            .setName(serializeName(enumEntry.name))

        if (!skipMutableState) {
            enumEntry.initializerExpression?.let {
                proto.initializer = serializeIrExpressionBody(it.expression)
            }
            enumEntry.correspondingClass?.let {
                proto.correspondingClass = serializeIrClass(it)
            }
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
            is IrErrorDeclaration ->
                proto.irErrorDeclaration = serializeIrErrorDeclaration(declaration)
            else ->
                TODO("Declaration serialization not supported yet: $declaration")
        }

        return proto.build()
    }

// ---------- Top level ------------------------------------------------------

    private fun serializeFileEntry(entry: IrFileEntry): ProtoFileEntry = ProtoFileEntry.newBuilder()
        .setName(entry.name)
        .addAllLineStartOffset(entry.lineStartOffsets.asIterable())
        .build()

    open fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean = false
    open fun keepOrderOfProperties(property: IrProperty): Boolean = !property.isConst
    open fun backendSpecificSerializeAllMembers(irClass: IrClass) = false

    fun memberNeedsSerialization(member: IrDeclaration): Boolean {
        assert(member.parent is IrClass)
        if (backendSpecificSerializeAllMembers(member.parent as IrClass)) return true

        return (!member.isFakeOverride)
    }

    private fun fillPlatformExplicitlyExported(file: IrFile, proto: ProtoFile.Builder) {

        if (backendSpecificExplicitRoot(file)) {
            for (declaration in file.declarations) {
                proto.addExplicitlyExportedToCompiler(serializeIrSymbol(declaration.symbol))
            }
        } else {
            file.acceptVoid(
                object : IrElementVisitorVoid {
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

    fun serializeDeclarationsForIC(file: IrFile, declarations: Iterable<IrDeclaration>): SerializedIrFile {
        val proto = ProtoFile.newBuilder()
            .setFileEntry(serializeFileEntry(file.fileEntry))
            .addAllFqName(serializeFqName(file.fqName.asString()))

        val topLevelDeclarations = mutableListOf<SerializedDeclaration>()

        for (declaration in declarations) {
            val byteArray = serializeDeclaration(declaration).toByteArray()
            val idSig = declarationTable.signatureByDeclaration(declaration)

            // TODO: keep order similar
            // ^ TODO what does that mean?
            val sigIndex = protoIdSignatureMap[idSig]
                ?: if (declaration is IrErrorDeclaration) protoIdSignature(idSig) else error("Not found ID for $idSig (${declaration.render()})")

            topLevelDeclarations.add(TopLevelDeclaration(sigIndex, idSig.toString(), byteArray))
        }

        topLevelDeclarations.forEach {
            proto.addDeclarationId(it.id)
        }

        return SerializedIrFile(
            proto.build().toByteArray(),
            file.fqName.asString(),
            file.path,
            IrMemoryArrayWriter(protoTypeArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryArrayWriter(protoIdSignatureArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryArrayWriter(protoStringArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryArrayWriter(protoBodyArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryDeclarationWriter(topLevelDeclarations).writeIntoMemory()
        )
    }

    fun serializeIrFile(file: IrFile): SerializedIrFile {
        val topLevelDeclarations = mutableListOf<SerializedDeclaration>()

        val proto = ProtoFile.newBuilder()
            .setFileEntry(serializeFileEntry(file.fileEntry))
            .addAllFqName(serializeFqName(file.fqName.asString()))
            .addAllAnnotation(serializeAnnotations(file.annotations))

        file.declarations.forEach {
            if (skipExpects && it.descriptor.isExpectMember && !it.descriptor.isSerializableExpectClass) {
                topLevelDeclarations.add(SkippedDeclaration)
                return@forEach
            }

            val byteArray = serializeDeclaration(it).toByteArray()
            val idSig = declarationTable.signatureByDeclaration(it)
            require(idSig === idSig.topLevelSignature()) { "IdSig: $idSig\ntopLevel: ${idSig.topLevelSignature()}" }
            require(!idSig.isPackageSignature()) { "IsSig: $idSig\nDeclaration: ${it.render()}" }

            // TODO: keep order similar
            val sigIndex = protoIdSignatureMap[idSig]
                ?: if (it is IrErrorDeclaration) protoIdSignature(idSig) else error("Not found ID for $idSig (${it.render()})")
            topLevelDeclarations.add(TopLevelDeclaration(sigIndex, idSig.toString(), byteArray))
            proto.addDeclarationId(sigIndex)
        }

        // TODO: is it Konan specific?

        // Make sure that all top level properties are initialized on library's load.
        file.declarations
            .filterIsInstance<IrProperty>()
            .filter { it.backingField?.initializer != null && keepOrderOfProperties(it) }
            .forEach {
                val fieldSymbol = it.backingField?.symbol ?: error("Not found ID ${it.render()}")
                proto.addExplicitlyExportedToCompiler(serializeIrSymbol(fieldSymbol))
            }

        fillPlatformExplicitlyExported(file, proto)
        serializeExpectActualSubstitutionTable(proto)

        return SerializedIrFile(
            proto.build().toByteArray(),
            file.fqName.asString(),
            file.path,
            IrMemoryArrayWriter(protoTypeArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryArrayWriter(protoIdSignatureArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryStringWriter(protoStringArray).writeIntoMemory(),
            IrMemoryArrayWriter(protoBodyArray.map { it.toByteArray() }).writeIntoMemory(),
            IrMemoryDeclarationWriter(topLevelDeclarations).writeIntoMemory()
        )
    }

    fun serializeExpectActualSubstitutionTable(proto: ProtoFile.Builder) {
        if (skipExpects) return

        expectActualTable.table.forEach next@{ (expect, actualSymbol) ->
            val expectSymbol = expectDescriptorToSymbol[expect] ?: error("Could not find expect symbol for expect descriptor $expect")

            proto.addActual(
                ProtoActual.newBuilder()
                    .setExpectSymbol(serializeIrSymbol(expectSymbol))
                    .setActualSymbol(serializeIrSymbol(actualSymbol))
                    .build()
            )
        }
    }
}
