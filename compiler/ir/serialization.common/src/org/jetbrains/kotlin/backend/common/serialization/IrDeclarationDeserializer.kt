/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkErrorNodesAllowed
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkSymbolType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType.KindCase.*
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrPublicSymbolBase
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.collections.set
import org.jetbrains.kotlin.backend.common.serialization.proto.IrAnonymousInit as ProtoAnonymousInit
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructor as ProtoConstructor
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase as ProtoDeclarationBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDefinitelyNotNullType as ProtoDefinitelyNotNullType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicType as ProtoDynamicType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumEntry as ProtoEnumEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorDeclaration as ProtoErrorDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorType as ProtoErrorType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrField as ProtoField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInlineClassRepresentation as ProtoIrInlineClassRepresentation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedProperty as ProtoLocalDelegatedProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleType as ProtoSimpleType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAbbreviation as ProtoTypeAbbreviation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAlias as ProtoTypeAlias
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter as ProtoTypeParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoValueParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVariable as ProtoVariable

class IrDeclarationDeserializer(
    builtIns: IrBuiltIns,
    private val symbolTable: SymbolTable,
    val irFactory: IrFactory,
    private val libraryFile: IrLibraryFile,
    private val parent: IrDeclarationParent,
    val allowErrorNodes: Boolean,
    private val deserializeInlineFunctions: Boolean,
    private var deserializeBodies: Boolean,
    val symbolDeserializer: IrSymbolDeserializer,
    private val platformFakeOverrideClassFilter: FakeOverrideClassFilter,
    private val fakeOverrideBuilder: FakeOverrideBuilder,
    private val skipMutableState: Boolean = false,
    additionalStatementOriginIndex: Map<String, IrStatementOrigin> = emptyMap(),
    allowErrorStatementOrigins: Boolean = false,
    private val compatibilityMode: CompatibilityMode,
    private val allowRedeclaration: Boolean = false,
    allowErrorLoopIndices: Boolean = false,
) {

    val bodyDeserializer = IrBodyDeserializer(
        builtIns,
        allowErrorNodes,
        irFactory,
        libraryFile,
        this,
        statementOriginIndex + additionalStatementOriginIndex,
        allowErrorStatementOrigins,
        allowErrorLoopIndices,
    )

    private fun deserializeName(index: Int): Name {
        val name = libraryFile.string(index)
        return Name.guessByFirstCharacter(name)
    }

    private val irTypeCache = mutableMapOf<Int, IrType>()

    private fun loadTypeProto(index: Int): ProtoType {
        return libraryFile.type(index)
    }

    fun deserializeNullableIrType(index: Int): IrType? = if (index == -1) null else deserializeIrType(index)

    fun deserializeIrType(index: Int): IrType {
        return irTypeCache.getOrPut(index) {
            val typeData = loadTypeProto(index)
            deserializeIrTypeData(typeData)
        }
    }

    private fun deserializeIrTypeArgument(proto: Long): IrTypeArgument {
        val encoding = BinaryTypeProjection.decode(proto)

        if (encoding.isStarProjection) return IrStarProjectionImpl

        return makeTypeProjection(deserializeIrType(encoding.typeIndex), encoding.variance)
    }

    internal fun deserializeAnnotations(annotations: List<ProtoConstructorCall>): List<IrConstructorCall> {
        return annotations.map {
            bodyDeserializer.deserializeAnnotation(it)
        }
    }

    private fun deserializeSimpleType(proto: ProtoSimpleType): IrSimpleType {
        val symbol = checkSymbolType<IrClassifierSymbol>(deserializeIrSymbolAndRemap(proto.classifier))

        val arguments = proto.argumentList.map { deserializeIrTypeArgument(it) }
        val annotations = deserializeAnnotations(proto.annotationList)

        return IrSimpleTypeImpl(
            null,
            symbol,
            proto.hasQuestionMark,
            arguments,
            annotations,
            if (proto.hasAbbreviation()) deserializeTypeAbbreviation(proto.abbreviation) else null
        )
    }

    private fun deserializeTypeAbbreviation(proto: ProtoTypeAbbreviation): IrTypeAbbreviation =
        IrTypeAbbreviationImpl(
            checkSymbolType(deserializeIrSymbolAndRemap(proto.typeAlias)),
            proto.hasQuestionMark,
            proto.argumentList.map { deserializeIrTypeArgument(it) },
            deserializeAnnotations(proto.annotationList)
        )

    private fun deserializeDynamicType(proto: ProtoDynamicType): IrDynamicType {
        val annotations = deserializeAnnotations(proto.annotationList)
        return IrDynamicTypeImpl(null, annotations, Variance.INVARIANT)
    }

    private fun deserializeErrorType(proto: ProtoErrorType): IrErrorType {
        checkErrorNodesAllowed<IrErrorType>(allowErrorNodes)
        val annotations = deserializeAnnotations(proto.annotationList)
        return IrErrorTypeImpl(null, annotations, Variance.INVARIANT)
    }

    private fun deserializeDefinitelyNotNullType(proto: ProtoDefinitelyNotNullType): IrDefinitelyNotNullType {
        assert(proto.typesCount == 1) { "Only DefinitelyNotNull type is now supported" }
        // TODO support general case of intersection type
        return IrDefinitelyNotNullTypeImpl(null, deserializeIrType(proto.typesList[0]))
    }

    private fun deserializeIrTypeData(proto: ProtoType): IrType {
        return when (proto.kindCase) {
            DNN -> deserializeDefinitelyNotNullType(proto.dnn)
            SIMPLE -> deserializeSimpleType(proto.simple)
            DYNAMIC -> deserializeDynamicType(proto.dynamic)
            ERROR -> deserializeErrorType(proto.error)
            else -> error("Unexpected IrType kind: ${proto.kindCase}")
        }
    }

    private var currentParent: IrDeclarationParent = parent

    private inline fun <T : IrDeclarationParent> T.usingParent(block: T.() -> Unit): T =
        this.apply {
            val oldParent = currentParent
            currentParent = this
            try {
                block(this)
            } finally {
                currentParent = oldParent
            }
        }

    // Delegating symbol maps to it's delegate only inside the declaration the symbol belongs to.
    private val delegatedSymbolMap = mutableMapOf<IrSymbol, IrSymbol>()

    internal fun deserializeIrSymbol(code: Long): IrSymbol {
        return symbolDeserializer.deserializeIrSymbol(code)
    }

    internal fun deserializeIrSymbolAndRemap(code: Long): IrSymbol {
        // TODO: could be simplified
        return symbolDeserializer.deserializeIrSymbol(code).let {
            delegatedSymbolMap[it] ?: it
        }
    }

    private fun recordDelegatedSymbol(symbol: IrSymbol) {
        if (symbol is IrDelegatingSymbol<*, *, *>) {
            delegatedSymbolMap[symbol] = symbol.delegate
        }
    }

    private fun eraseDelegatedSymbol(symbol: IrSymbol) {
        if (symbol is IrDelegatingSymbol<*, *, *>) {
            delegatedSymbolMap.remove(symbol)
        }
    }

    private var isEffectivelyExternal = false

    private inline fun withExternalValue(value: Boolean, fn: () -> Unit) {
        val oldExternalValue = isEffectivelyExternal
        isEffectivelyExternal = value
        try {
            fn()
        } finally {
            isEffectivelyExternal = oldExternalValue
        }
    }

    private inline fun <T> withDeserializedIrDeclarationBase(
        proto: ProtoDeclarationBase,
        block: (IrSymbol, IdSignature, Int, Int, IrDeclarationOrigin, Long) -> T
    ): T where T : IrDeclaration, T : IrSymbolOwner {
        val (s, uid) = symbolDeserializer.deserializeIrSymbolToDeclare(proto.symbol)
        val coordinates = BinaryCoordinates.decode(proto.coordinates)
        try {
            recordDelegatedSymbol(s)
            val result = block(
                s,
                uid,
                coordinates.startOffset, coordinates.endOffset,
                deserializeIrDeclarationOrigin(proto.originName), proto.flags
            )
            // avoid duplicate annotations for local variables
            if (!allowRedeclaration || result.annotations.isEmpty()) {
                result.annotations = deserializeAnnotations(proto.annotationList)
            }
            if (!skipMutableState) {
                result.parent = currentParent
            }
            return result
        } finally {
            eraseDelegatedSymbol(s)
        }
    }

    private fun deserializeIrTypeParameter(proto: ProtoTypeParameter, index: Int, isGlobal: Boolean): IrTypeParameter {
        val name = deserializeName(proto.name)
        val coordinates = BinaryCoordinates.decode(proto.base.coordinates)
        val flags = TypeParameterFlags.decode(proto.base.flags)

        val factory = { symbol: IrTypeParameterSymbol ->
            irFactory.createTypeParameter(
                coordinates.startOffset,
                coordinates.endOffset,
                deserializeIrDeclarationOrigin(proto.base.originName),
                symbol,
                name,
                index,
                flags.isReified,
                flags.variance
            )
        }

        val sig: IdSignature
        val result = symbolTable.run {
            if (isGlobal) {
                val p = symbolDeserializer.deserializeIrSymbolToDeclare(proto.base.symbol)
                val symbol = checkSymbolType<IrTypeParameterSymbol>(p.first)
                sig = p.second
                declareGlobalTypeParameter(sig, { symbol }, factory)
            } else {
                val symbolData = BinarySymbolData.decode(proto.base.symbol)
                sig = symbolDeserializer.deserializeIdSignature(symbolData.signatureId)
                declareScopedTypeParameter(sig, {
                    if (it.isPubliclyVisible) IrTypeParameterPublicSymbolImpl(it) else IrTypeParameterSymbolImpl() }, factory)
            }
        }

        // make sure this symbol is known to linker
        symbolDeserializer.referenceLocalIrSymbol(result.symbol, sig)
        result.annotations = deserializeAnnotations(proto.base.annotationList)
        result.parent = currentParent
        return result
    }

    private fun deserializeIrValueParameter(proto: ProtoValueParameter, index: Int): IrValueParameter =
        withDeserializedIrDeclarationBase(proto.base) { symbol, _, startOffset, endOffset, origin, fcode ->
            val flags = ValueParameterFlags.decode(fcode)
            val nameAndType = BinaryNameAndType.decode(proto.nameType)
            irFactory.createValueParameter(
                startOffset, endOffset, origin,
                checkSymbolType(symbol),
                deserializeName(nameAndType.nameIndex),
                index,
                deserializeIrType(nameAndType.typeIndex),
                if (proto.hasVarargElementType()) deserializeIrType(proto.varargElementType) else null,
                flags.isCrossInline,
                flags.isNoInline,
                flags.isHidden,
                flags.isAssignable
            ).apply {
                if (proto.hasDefaultValue())
                    defaultValue = deserializeExpressionBody(proto.defaultValue)
                        ?: irFactory.createExpressionBody(IrCompositeImpl(startOffset, endOffset, type))
            }
        }

    fun deserializeIrClass(proto: ProtoClass): IrClass =
        withDeserializedIrDeclarationBase(proto.base) { symbol, signature, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrClassSymbol>(symbol)
            if (allowRedeclaration && symbol.isBound) return symbol.owner

            val flags = ClassFlags.decode(fcode)
            symbolTable.declareClass(signature, { symbol }) {
                irFactory.createClass(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(proto.name),
                    flags.kind,
                    flags.visibility,
                    flags.modality,
                    flags.isCompanion,
                    flags.isInner,
                    flags.isData,
                    flags.isExternal || isEffectivelyExternal,
                    flags.isInline,
                    flags.isExpect,
                    flags.isFun,
                )
            }.usingParent {
                if (!skipMutableState) {
                    typeParameters = deserializeTypeParameters(proto.typeParameterList, true)

                    superTypes = proto.superTypeList.map { deserializeIrType(it) }

                    withExternalValue(isExternal) {
                        val oldDeclarations = declarations.toSet()
                        proto.declarationList
                            .filterNot { isSkippableFakeOverride(it, this) }
                            // On JVM, deserialization may fill bodies of existing declarations, so avoid adding duplicates.
                            .mapNotNullTo(declarations) { declProto -> deserializeDeclaration(declProto).takeIf { it !in oldDeclarations } }
                    }

                    thisReceiver = deserializeIrValueParameter(proto.thisReceiver, -1)

                    inlineClassRepresentation = when {
                        !flags.isInline -> null
                        proto.hasInlineClassRepresentation() -> deserializeInlineClassRepresentation(proto.inlineClassRepresentation)
                        else -> computeMissingInlineClassRepresentationForCompatibility(this)
                    }

                    sealedSubclasses = proto.sealedSubclassList.map { deserializeIrSymbol(it) as IrClassSymbol }

                    fakeOverrideBuilder.enqueueClass(this, signature, compatibilityMode)
                }
            }
        }

    fun deserializeInlineClassRepresentation(proto: ProtoIrInlineClassRepresentation): InlineClassRepresentation<IrSimpleType> =
        InlineClassRepresentation(
            deserializeName(proto.underlyingPropertyName),
            deserializeIrType(proto.underlyingPropertyType) as IrSimpleType,
        )

    private fun computeMissingInlineClassRepresentationForCompatibility(irClass: IrClass): InlineClassRepresentation<IrSimpleType> {
        // For inline classes compiled with 1.5.20 or earlier, try to reconstruct inline class representation from the single parameter of
        // the primary constructor. Something similar is happening in `DeserializedClassDescriptor.computeInlineClassRepresentation`.
        // This code will be unnecessary as soon as klibs compiled with Kotlin 1.5.20 are no longer supported.
        val ctor = irClass.primaryConstructor ?: error("Inline class has no primary constructor: ${irClass.render()}")
        val parameter =
            ctor.valueParameters.singleOrNull() ?: error("Failed to get single parameter of inline class constructor: ${ctor.render()}")
        return InlineClassRepresentation(parameter.name, parameter.type as IrSimpleType)
    }

    private fun deserializeIrTypeAlias(proto: ProtoTypeAlias): IrTypeAlias =
        withDeserializedIrDeclarationBase(proto.base) { symbol, uniqId, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrTypeAliasSymbol>(symbol)
            symbolTable.declareTypeAlias(uniqId, { symbol }) {
                val flags = TypeAliasFlags.decode(fcode)
                val nameType = BinaryNameAndType.decode(proto.nameType)
                irFactory.createTypeAlias(
                    startOffset, endOffset,
                    it,
                    deserializeName(nameType.nameIndex),
                    flags.visibility,
                    deserializeIrType(nameType.typeIndex),
                    flags.isActual,
                    origin
                )
            }.usingParent {
                if (!skipMutableState) {
                    typeParameters = deserializeTypeParameters(proto.typeParameterList, true)
                }
            }
        }

    private fun deserializeErrorDeclaration(proto: ProtoErrorDeclaration): IrErrorDeclaration {
        checkErrorNodesAllowed<IrErrorDeclaration>(allowErrorNodes)
        val coordinates = BinaryCoordinates.decode(proto.coordinates)
        return irFactory.createErrorDeclaration(coordinates.startOffset, coordinates.endOffset).also {
            it.parent = currentParent
        }
    }

    private fun deserializeTypeParameters(protos: List<ProtoTypeParameter>, isGlobal: Boolean): List<IrTypeParameter> {
        // NOTE: fun <C : MutableCollection<in T>, T : Any> Array<out T?>.filterNotNullTo(destination: C): C
        val result = ArrayList<IrTypeParameter>(protos.size)
        for (index in protos.indices) {
            val proto = protos[index]
            result.add(deserializeIrTypeParameter(proto, index, isGlobal))
        }

        for (i in protos.indices) {
            result[i].superTypes = protos[i].superTypeList.map { deserializeIrType(it) }
        }

        return result
    }

    private fun deserializeValueParameters(protos: List<ProtoValueParameter>): List<IrValueParameter> {
        val result = ArrayList<IrValueParameter>(protos.size)

        for (i in protos.indices) {
            result.add(deserializeIrValueParameter(protos[i], i))
        }

        return result
    }


    /**
     * In `declarations-only` mode in case of private property/function with inferred anonymous private type like this
     * class C {
     *   private val p = object {
     *     fun foo() = 42
     *   }
     *
     *   private fun f() = object {
     *     fun bar() = "42"
     *   }
     *
     *   private val pp = p.foo()
     *   private fun ff() = f().bar()
     * }
     * object's classifier is leaked outside p/f scopes and accessible on C's level so
     * if their initializer/body weren't read we have unbound `foo/bar` symbol and unbound `object` symbols.
     * To fix this make sure that such declaration forced to be deserialized completely.
     *
     * For more information see `anonymousClassLeak.kt` test and issue KT-40216
     */
    private fun IrType.checkObjectLeak(): Boolean {
        return if (this is IrSimpleType) {
            val signature = classifier.signature

            val possibleLeakedClassifier = (signature == null || signature.isLocal) && classifier !is IrTypeParameterSymbol

            possibleLeakedClassifier || arguments.any { it.typeOrNull?.checkObjectLeak() == true }
        } else false
    }

    private fun <T : IrFunction> T.withBodyGuard(block: T.() -> Unit) {
        val oldBodiesPolicy = deserializeBodies

        fun checkInlineBody(): Boolean = deserializeInlineFunctions && this is IrSimpleFunction && isInline

        try {
            deserializeBodies = oldBodiesPolicy || checkInlineBody() || returnType.checkObjectLeak()
            block()
        } finally {
            deserializeBodies = oldBodiesPolicy
        }
    }


    private fun IrField.withInitializerGuard(f: IrField.() -> Unit) {
        val oldBodiesPolicy = deserializeBodies

        try {
            deserializeBodies = oldBodiesPolicy || type.checkObjectLeak()
            f()
        } finally {
            deserializeBodies = oldBodiesPolicy
        }
    }

    private fun loadStatementBodyProto(index: Int): ProtoStatement {
        return libraryFile.statementBody(index)
    }

    private fun loadExpressionBodyProto(index: Int): ProtoExpression {
        return libraryFile.expressionBody(index)
    }

    fun deserializeExpressionBody(index: Int): IrExpressionBody? {
        return if (deserializeBodies) {
            val bodyData = loadExpressionBodyProto(index)
            irFactory.createExpressionBody(bodyDeserializer.deserializeExpression(bodyData))
        } else {
            null
        }
    }

    fun deserializeStatementBody(index: Int): IrElement? {
        return if (deserializeBodies) {
            val bodyData = loadStatementBodyProto(index)
            bodyDeserializer.deserializeStatement(bodyData)
        } else {
            null
        }
    }

    private inline fun <T : IrFunction> withDeserializedIrFunctionBase(
        proto: ProtoFunctionBase,
        block: (IrFunctionSymbol, IdSignature, Int, Int, IrDeclarationOrigin, Long) -> T
    ): T = withDeserializedIrDeclarationBase(proto.base) { symbol, idSig, startOffset, endOffset, origin, fcode ->
        symbolTable.withScope(symbol) {
            block(checkSymbolType(symbol), idSig, startOffset, endOffset, origin, fcode).usingParent {
                if (!skipMutableState) {
                    typeParameters = deserializeTypeParameters(proto.typeParameterList, false)
                    val nameType = BinaryNameAndType.decode(proto.nameType)
                    returnType = deserializeIrType(nameType.typeIndex)

                    withBodyGuard {
                        valueParameters = deserializeValueParameters(proto.valueParameterList)
                        dispatchReceiverParameter =
                            if (proto.hasDispatchReceiver()) deserializeIrValueParameter(proto.dispatchReceiver, -1)
                            else null
                        extensionReceiverParameter =
                            if (proto.hasExtensionReceiver()) deserializeIrValueParameter(proto.extensionReceiver, -1)
                            else null
                        body =
                            if (proto.hasBody()) deserializeStatementBody(proto.body) as IrBody?
                            else null
                    }
                }
            }
        }
    }

    internal fun deserializeIrFunction(proto: ProtoFunction): IrSimpleFunction {
        return withDeserializedIrFunctionBase(proto.base) { symbol, idSig, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrSimpleFunctionSymbol>(symbol)
            if (allowRedeclaration && symbol.isBound) return symbol.owner

            val flags = FunctionFlags.decode(fcode)
            symbolTable.declareSimpleFunction(idSig, { symbol }) {
                val nameType = BinaryNameAndType.decode(proto.base.nameType)
                irFactory.createFunction(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(nameType.nameIndex),
                    flags.visibility,
                    flags.modality,
                    IrUninitializedType,
                    flags.isInline,
                    flags.isExternal || isEffectivelyExternal,
                    flags.isTailrec,
                    flags.isSuspend,
                    flags.isOperator,
                    flags.isInfix,
                    flags.isExpect,
                    flags.isFakeOverride
                )
            }.apply {
                overriddenSymbols = proto.overriddenList.map { checkSymbolType(deserializeIrSymbolAndRemap(it)) }
            }
        }
    }

    fun deserializeIrVariable(proto: ProtoVariable): IrVariable =
        withDeserializedIrDeclarationBase(proto.base) { symbol, _, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrVariableSymbol>(symbol)

            val flags = LocalVariableFlags.decode(fcode)
            val nameType = BinaryNameAndType.decode(proto.nameType)

            (if (allowRedeclaration && symbol.isBound) symbol.owner else IrVariableImpl(
                startOffset, endOffset, origin,
                symbol,
                deserializeName(nameType.nameIndex),
                deserializeIrType(nameType.typeIndex),
                flags.isVar,
                flags.isConst,
                flags.isLateinit
            )).apply {
                if (proto.hasInitializer())
                    initializer = bodyDeserializer.deserializeExpression(proto.initializer)
            }
        }

    private fun deserializeIrEnumEntry(proto: ProtoEnumEntry): IrEnumEntry =
        withDeserializedIrDeclarationBase(proto.base) { symbol, uniqId, startOffset, endOffset, origin, _ ->
            symbolTable.declareEnumEntry(uniqId, { checkSymbolType(symbol) }) {
                irFactory.createEnumEntry(startOffset, endOffset, origin, it, deserializeName(proto.name))
            }.apply {
                if (!skipMutableState) {
                    if (proto.hasCorrespondingClass())
                        correspondingClass = deserializeIrClass(proto.correspondingClass)
                    if (proto.hasInitializer())
                        initializerExpression = deserializeExpressionBody(proto.initializer)
                }
            }
        }

    private fun deserializeIrAnonymousInit(proto: ProtoAnonymousInit): IrAnonymousInitializer =
        withDeserializedIrDeclarationBase(proto.base) { symbol, _, startOffset, endOffset, origin, _ ->
            irFactory.createAnonymousInitializer(startOffset, endOffset, origin, checkSymbolType(symbol)).apply {
                body = deserializeStatementBody(proto.body) as IrBlockBody? ?: irFactory.createBlockBody(startOffset, endOffset)
            }
        }

    private fun deserializeIrConstructor(proto: ProtoConstructor): IrConstructor =
        withDeserializedIrFunctionBase(proto.base) { symbol, idSig, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrConstructorSymbol>(symbol)
            val flags = FunctionFlags.decode(fcode)
            val nameType = BinaryNameAndType.decode(proto.base.nameType)
            symbolTable.declareConstructor(idSig, { symbol }) {
                irFactory.createConstructor(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(nameType.nameIndex),
                    flags.visibility,
                    IrUninitializedType,
                    flags.isInline,
                    flags.isExternal || isEffectivelyExternal,
                    flags.isPrimary,
                    flags.isExpect
                )
            }
        }


    private fun deserializeIrField(proto: ProtoField): IrField =
        withDeserializedIrDeclarationBase(proto.base) { symbol, uniqId, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrFieldSymbol>(symbol)
            val nameType = BinaryNameAndType.decode(proto.nameType)
            val type = deserializeIrType(nameType.typeIndex)
            val flags = FieldFlags.decode(fcode)

            val field = if (allowRedeclaration && symbol.isBound) symbol.owner else symbolTable.declareField(uniqId, { symbol }) {
                irFactory.createField(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(nameType.nameIndex),
                    type,
                    flags.visibility,
                    flags.isFinal,
                    flags.isExternal || isEffectivelyExternal,
                    flags.isStatic,
                )
            }

            field.usingParent {
                if (proto.hasInitializer()) {
                    withInitializerGuard {
                        initializer = deserializeExpressionBody(proto.initializer)
                    }
                }
            }

            field
        }

    private fun deserializeIrLocalDelegatedProperty(proto: ProtoLocalDelegatedProperty): IrLocalDelegatedProperty =
        withDeserializedIrDeclarationBase(proto.base) { symbol, _, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrLocalDelegatedPropertySymbol>(symbol)

            val flags = LocalVariableFlags.decode(fcode)
            val nameAndType = BinaryNameAndType.decode(proto.nameType)

            val prop = if (allowRedeclaration && symbol.isBound) symbol.owner else irFactory.createLocalDelegatedProperty(
                startOffset, endOffset, origin,
                symbol,
                deserializeName(nameAndType.nameIndex),
                deserializeIrType(nameAndType.typeIndex),
                flags.isVar
            )

            if (!skipMutableState) {
                prop.apply {
                    delegate = deserializeIrVariable(proto.delegate)
                    getter = deserializeIrFunction(proto.getter)
                    if (proto.hasSetter())
                        setter = deserializeIrFunction(proto.setter)
                }
            }

            prop
        }

    private fun deserializeIrProperty(proto: ProtoProperty): IrProperty =
        withDeserializedIrDeclarationBase(proto.base) { symbol, uniqId, startOffset, endOffset, origin, fcode ->
            checkSymbolType<IrPropertySymbol>(symbol)
            val flags = PropertyFlags.decode(fcode)
            val prop = if (allowRedeclaration && symbol.isBound) symbol.owner else symbolTable.declareProperty(uniqId, { symbol }) {
                irFactory.createProperty(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(proto.name),
                    flags.visibility,
                    flags.modality,
                    flags.isVar,
                    flags.isConst,
                    flags.isLateinit,
                    flags.isDelegated,
                    flags.isExternal || isEffectivelyExternal,
                    flags.isExpect,
                    flags.isFakeOverride
                )
            }

            if (!skipMutableState) {
                prop.apply {
                    withExternalValue(isExternal) {
                        if (proto.hasGetter()) {
                            getter = deserializeIrFunction(proto.getter).also {
                                it.correspondingPropertySymbol = symbol
                            }
                        }
                        if (proto.hasSetter()) {
                            setter = deserializeIrFunction(proto.setter).also {
                                it.correspondingPropertySymbol = symbol
                            }
                        }
                        if (proto.hasBackingField()) {
                            backingField = deserializeIrField(proto.backingField).also {
                                it.correspondingPropertySymbol = symbol
                            }
                        }
                    }
                }
            }

            prop
        }

    companion object {
        private val allKnownDeclarationOrigins = IrDeclarationOrigin::class.nestedClasses.toList()
        private val declarationOriginIndex =
            allKnownDeclarationOrigins.map { it.objectInstance as IrDeclarationOriginImpl }.associateBy { it.name }


        private val allKnownStatementOrigins = IrStatementOrigin::class.nestedClasses.toList()
        private val statementOriginIndex =
            allKnownStatementOrigins.mapNotNull { it.objectInstance as? IrStatementOriginImpl }.associateBy { it.debugName }
    }

    fun deserializeIrDeclarationOrigin(protoName: Int): IrDeclarationOriginImpl {
        val originName = libraryFile.string(protoName)
        return declarationOriginIndex[originName] ?: object : IrDeclarationOriginImpl(originName) {}
    }

    fun deserializeDeclaration(proto: ProtoDeclaration): IrDeclaration {
        val declaration: IrDeclaration = when (proto.declaratorCase!!) {
            IR_ANONYMOUS_INIT -> deserializeIrAnonymousInit(proto.irAnonymousInit)
            IR_CONSTRUCTOR -> deserializeIrConstructor(proto.irConstructor)
            IR_FIELD -> deserializeIrField(proto.irField)
            IR_CLASS -> deserializeIrClass(proto.irClass)
            IR_FUNCTION -> deserializeIrFunction(proto.irFunction)
            IR_PROPERTY -> deserializeIrProperty(proto.irProperty)
            IR_TYPE_PARAMETER -> deserializeIrTypeParameter(proto.irTypeParameter, proto.irTypeParameter.index, proto.irTypeParameter.isGlobal)
            IR_VARIABLE -> deserializeIrVariable(proto.irVariable)
            IR_VALUE_PARAMETER -> deserializeIrValueParameter(proto.irValueParameter, proto.irValueParameter.index)
            IR_ENUM_ENTRY -> deserializeIrEnumEntry(proto.irEnumEntry)
            IR_LOCAL_DELEGATED_PROPERTY -> deserializeIrLocalDelegatedProperty(proto.irLocalDelegatedProperty)
            IR_TYPE_ALIAS -> deserializeIrTypeAlias(proto.irTypeAlias)
            IR_ERROR_DECLARATION -> deserializeErrorDeclaration(proto.irErrorDeclaration)
            DECLARATOR_NOT_SET -> error("Declaration deserialization not implemented: ${proto.declaratorCase}")
        }

        return declaration
    }

    // Depending on deserialization strategy we either deserialize public api fake overrides
    // or reconstruct them after IR linker completes.
    private fun isSkippableFakeOverride(proto: ProtoDeclaration, parent: IrClass): Boolean {
        if (!platformFakeOverrideClassFilter.needToConstructFakeOverrides(parent)) return false

        val symbol = when (proto.declaratorCase!!) {
            IR_FUNCTION -> symbolDeserializer.deserializeIrSymbol(proto.irFunction.base.base.symbol)
            IR_PROPERTY -> symbolDeserializer.deserializeIrSymbol(proto.irProperty.base.symbol)
            // Don't consider IR_FIELDS here.
            else -> return false
        }
        if (symbol !is IrPublicSymbolBase<*>) return false
        if (!symbol.signature.isPubliclyVisible) return false

        return when (proto.declaratorCase!!) {
            IR_FUNCTION -> FunctionFlags.decode(proto.irFunction.base.base.flags).isFakeOverride
            IR_PROPERTY -> PropertyFlags.decode(proto.irProperty.base.flags).isFakeOverride
            // Don't consider IR_FIELDS here.
            else -> false
        }
    }
}