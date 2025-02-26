/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrDeserializationSettings.DeserializeFunctionBodies
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class NonLinkingIrInlineFunctionDeserializer(
    private val irBuiltIns: IrBuiltIns,
    private val signatureComputer: PublicIdSignatureComputer,
) {
    private val irInterner = IrInterningService()
    private val irFactory get() = irBuiltIns.irFactory

    /**
     * This is a separate symbol table ("detached") from the symbol table ("main") that is used in IR linker.
     *
     * The goal is to separate the linkage process, which should end with all symbols been bound to the respective declarations,
     * and the process of partial deserialization of inline functions, which should produce some amount of unbound symbols
     * that are not supposed to be linked and therefore should not be tracked in the main symbol table.
     */
    private val detachedSymbolTable = SymbolTable(signaturer = null, irFactory)

    private val moduleDeserializers = hashMapOf<KotlinLibrary, ModuleDeserializer>()

    // TODO: consider the case of `external inline` functions that exist in Kotlin/Native stdlib
    fun deserializeInlineFunction(function: IrFunction) {
        check(function.isInline) { "Non-inline function: ${function.render()}" }
        check(!function.isFakeOverride) { "Deserialization of fake overrides is not supported: ${function.render()}" }

        if (function.body != null) return

        check(!function.isEffectivelyPrivate()) { "Deserialization of private inline functions is not supported: ${function.render()}" }

        resetSymbolTable()
        val deserializedContainerSource = function.containerSource
        check(deserializedContainerSource is KlibDeserializedContainerSource) {
            "Cannot deserialize inline function from a non-Kotlin library: ${function.render()}\nFunction source: " +
                    deserializedContainerSource?.let { "${it::class.java}, ${it.presentableString}" }
        }

        val library = deserializedContainerSource.klib
        val moduleDeserializer = moduleDeserializers.getOrPut(library) { ModuleDeserializer(library) }

        val functionSignature: IdSignature = signatureComputer.computeSignature(function)
        val topLevelSignature: IdSignature = functionSignature.topLevelSignature()

        val topLevelDeclaration: IrDeclaration? = moduleDeserializer.getTopLevelDeclarationOrNull(topLevelSignature)

        val deserializedFunction: IrFunction? = when {
            topLevelDeclaration == null -> null

            functionSignature == topLevelSignature -> topLevelDeclaration as? IrFunction

            else -> {
                val symbol = referencePublicSymbol(functionSignature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL)
                runIf(symbol.isBound) { symbol.owner as IrFunction }
            }
        }

        check(deserializedFunction != null) { "Inline function is not found: $functionSignature" }

        val deserializedBody: IrBody? = deserializedFunction.body
        check(deserializedBody != null) { "Deserialized inline function has no body: $functionSignature" }

        val deserializedDefaultValues: List<IrExpressionBody?> = deserializedFunction.parameters.map(IrValueParameter::defaultValue)

        // Collect type/value parameter symbols that are referenced inside the deserialized function body, but point to
        // type/value parameters of the deserialized function or one of it's containing classes (i.e., instance receiver value parameter).
        // Such symbols should be substituted by the corresponding symbols of the original function and it's containing classes.
        val typeAndValueParameterSymbolsToRemap = collectExternalTypeAndValueParameterSymbolsToRemap(
            originalBodilessFunction = function,
            deserializedFunction = deserializedFunction
        )

        // Traverse all declarations inside the deserialized function body. Memoize their symbols and create new (unbound) symbols
        // that will be used as a substitution during copying. This step needs to be done in advance before copying.
        val symbolRemapper = object : DeepCopySymbolRemapper() {
            init {
                if (deserializedFunction is IrSimpleFunction && function is IrSimpleFunction) {
                    // The function symbol can be referenced in return statements:
                    functions[deserializedFunction.symbol] = function.symbol
                }

                // Type and value parameters can be referenced in expressions:
                typeParameters += typeAndValueParameterSymbolsToRemap.typeParameterSymbols
                valueParameters += typeAndValueParameterSymbolsToRemap.valueParameterSymbols
            }
        }
        deserializedBody.acceptVoid(symbolRemapper)
        deserializedDefaultValues.forEach { it?.acceptVoid(symbolRemapper) }

        // Make a deep copy of the function body. Bind symbols created at the previous step to new (copied) declarations.
        // IMPORTANT: How referenced symbols are remapped:
        // 1. If the symbol corresponds to the declaration inside the body, then it's substituted by the symbol of that declaration's copy.
        // 2. If the symbol corresponds to the value parameter outside the body, then it's substituted by the corresponding value parameter
        //    symbol according to `valueParameterSymbolsToRemap`.
        // 3. Otherwise, if the symbol has a public signature and is bound, then the symbol is copied preserving the signature and left unbound.
        //    This is necessary to prevent having references to duplicated entities coming from the deserialized IR.
        val leakagePreventingSymbolRemapper = DeserializedDeclarationLeakagePreventingSymbolRemapper(symbolRemapper)
        val deepCopyTransformer = DeepCopyIrTreeWithSymbols(
            leakagePreventingSymbolRemapper,
            DeepCopyTypeRemapper(leakagePreventingSymbolRemapper)
        )
        val bodyWithRemappedSymbols: IrBody = deserializedBody.transform(deepCopyTransformer, null)
        val defaultValuesWithRemappedSymbols: List<IrExpressionBody?> =
            deserializedDefaultValues.map { it?.transform(deepCopyTransformer, null) }

        // Avoid having a reference to the deserialized function through `IrDeclaration.parent`.
        bodyWithRemappedSymbols.patchDeclarationParents(function)
        defaultValuesWithRemappedSymbols.forEach { it?.patchDeclarationParents(function) }

        function.body = bodyWithRemappedSymbols
        function.parameters.forEachIndexed { index, parameter -> parameter.defaultValue = defaultValuesWithRemappedSymbols[index] }
        function.parentsWithSelf.forEach {
            if (it is IrDeclaration && it.parent is IrExternalPackageFragment) {
                val deserializedFile = deserializedFunction.file
                it.parent = IrFileImpl(
                    fileEntry = deserializedFile.fileEntry,
                    symbol = IrFileSymbolImpl(),
                    packageFqName = deserializedFile.packageFqName
                )
            }
        }
    }

    @OptIn(DelicateSymbolTableApi::class)
    private fun resetSymbolTable() {
        detachedSymbolTable.reset()
    }

    private fun referencePublicSymbol(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind) =
        referenceDeserializedSymbol(detachedSymbolTable, fileSymbol = null, symbolKind, signature)

    private inner class ModuleDeserializer(library: KotlinLibrary) {
        init {
            check(library.hasIr) { "Ir-less library: ${library.libraryFile.path}" }
        }

        private val fileDeserializers = (0 until library.fileCount()).map { fileIndex ->
            FileDeserializer(library, fileIndex)
        }

        fun getTopLevelDeclarationOrNull(topLevelSignature: IdSignature): IrDeclaration? =
            fileDeserializers.firstNotNullOfOrNull { it.getTopLevelDeclarationOrNull(topLevelSignature) }
    }

    private inner class FileDeserializer(library: KotlinLibrary, fileIndex: Int) {
        private val fileProto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))

        private val dummyFileSymbol = IrFileSymbolImpl().apply {
            val fileEntry = library.fileEntry(fileProto, fileIndex)
            IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(fileEntry.name, fileProto.fileEntry.lineStartOffsetList.toIntArray()),
                symbol = this,
                packageFqName = FqName(irInterner.string(fileReader.deserializeFqName(fileProto.fqNameList)))
            )
        }

        private val symbolDeserializer = IrSymbolDeserializer(
            detachedSymbolTable,
            fileReader,
            dummyFileSymbol,
            enqueueLocalTopLevelDeclaration = {},
            irInterner,
            deserializePublicSymbol = ::referencePublicSymbol,
            createFOSymbolForLocalFO = true,
        )

        private val declarationDeserializer = IrDeclarationDeserializer(
            builtIns = irBuiltIns,
            symbolTable = detachedSymbolTable,
            irFactory = irFactory,
            libraryFile = fileReader,
            parent = dummyFileSymbol.owner,
            settings = IrDeserializationSettings(
                deserializeFunctionBodies = DeserializeFunctionBodies.ONLY_INLINE,
                useNullableAnyAsAnnotationConstructorCallType = true,
            ),
            symbolDeserializer = symbolDeserializer,
            onDeserializedClass = { _, _ -> },
            needToDeserializeFakeOverrides = { false },
            specialProcessingForMismatchedSymbolKind = null,
            irInterner = irInterner,
        )

        /**
         * Deserialize declarations only on demand. Cache top-level declarations to avoid repetitive deserialization
         * if the declaration happens to have multiple inline functions.
         */
        private val indexWithLazyValues: Map<IdSignature, Lazy<IrDeclaration>> = fileProto.declarationIdList.associate { declarationId ->
            val signature = symbolDeserializer.deserializeIdSignature(declarationId)

            val lazyDeclaration = lazy {
                val declarationProto = fileReader.declaration(declarationId)
                declarationDeserializer.deserializeDeclaration(declarationProto)
            }

            signature to lazyDeclaration
        }

        fun getTopLevelDeclarationOrNull(topLevelSignature: IdSignature): IrDeclaration? = indexWithLazyValues[topLevelSignature]?.value
    }
}

private data class TypeAndValueParameterSymbolsToRemap(
    val typeParameterSymbols: Map</* deserialized */ IrTypeParameterSymbol, /* original */ IrTypeParameterSymbol>,
    val valueParameterSymbols: Map</* deserialized */ IrValueParameterSymbol, /* original */ IrValueParameterSymbol>,
)

private fun collectExternalTypeAndValueParameterSymbolsToRemap(
    originalBodilessFunction: IrFunction,
    deserializedFunction: IrFunction,
): TypeAndValueParameterSymbolsToRemap {

    class TypeAndValueParameterSymbolsToRemapForSingleDeclaration(val declaration: IrDeclaration) {
        val typeParameterSymbols: List<IrTypeParameterSymbol>
        val valueParameterSymbols: List<IrValueParameterSymbol>

        init {
            when (declaration) {
                is IrFunction -> {
                    typeParameterSymbols = declaration.typeParameters.map(IrTypeParameter::symbol)
                    valueParameterSymbols = declaration.parameters.map(IrValueParameter::symbol)
                }
                is IrClass -> {
                    typeParameterSymbols = declaration.typeParameters.map(IrTypeParameter::symbol)
                    valueParameterSymbols = listOfNotNull(declaration.thisReceiver?.symbol)
                }
                else -> {
                    typeParameterSymbols = emptyList()
                    valueParameterSymbols = emptyList()
                }
            }
        }

        fun matches(other: TypeAndValueParameterSymbolsToRemapForSingleDeclaration): Boolean =
            typeParameterSymbols.size == other.typeParameterSymbols.size &&
                    valueParameterSymbols.size == other.valueParameterSymbols.size
    }

    fun collectSymbolsFrom(function: IrFunction): List<TypeAndValueParameterSymbolsToRemapForSingleDeclaration> {
        val result = ArrayList<TypeAndValueParameterSymbolsToRemapForSingleDeclaration>()

        for (declaration in function.parentDeclarationsWithSelf) {
            result += TypeAndValueParameterSymbolsToRemapForSingleDeclaration(declaration)

            if (declaration is IrClass && !declaration.isInner)
                break
        }

        return result
    }

    val originalBodilessFunctionSymbolsByLevels = collectSymbolsFrom(originalBodilessFunction)
    val deserializedFunctionSymbolsByLevels = collectSymbolsFrom(deserializedFunction)

    fun generateErrorMessage(): String = buildString {
        append("The set of type/value parameters that can be referenced inside the body of the inline function ")
        append(deserializedFunction.symbol.signature)
        appendLine(" differs between the bodiless function and the deserialized function.")

        appendLine("Bodiless function:")
        for (symbolsAtSameLevel in originalBodilessFunctionSymbolsByLevels) {
            appendLine("\tDeclaration: ${symbolsAtSameLevel.declaration.render()}")
            appendLine("\t\tType parameters: ${symbolsAtSameLevel.typeParameterSymbols.joinToString()}")
            appendLine("\t\tValue parameters: ${symbolsAtSameLevel.valueParameterSymbols.joinToString()}")
        }

        appendLine("Deserialized function:")
        for (symbolsAtSameLevel in deserializedFunctionSymbolsByLevels) {
            appendLine("\tDeclaration: ${symbolsAtSameLevel.declaration.render()}")
            appendLine("\t\tType parameters: ${symbolsAtSameLevel.typeParameterSymbols.joinToString()}")
            appendLine("\t\tValue parameters: ${symbolsAtSameLevel.valueParameterSymbols.joinToString()}")
        }
    }

    check(originalBodilessFunctionSymbolsByLevels.size == deserializedFunctionSymbolsByLevels.size, ::generateErrorMessage)

    val typeParameterSymbolsForRemapping: MutableMap<IrTypeParameterSymbol, IrTypeParameterSymbol> = hashMapOf()
    val valueParameterSymbolsForRemapping: MutableMap<IrValueParameterSymbol, IrValueParameterSymbol> = hashMapOf()

    for (levelIndex in originalBodilessFunctionSymbolsByLevels.indices) {
        val originalSymbolsAtSameLevel = originalBodilessFunctionSymbolsByLevels[levelIndex]
        val deserializedSymbolsAtSameLevel = deserializedFunctionSymbolsByLevels[levelIndex]

        check(originalSymbolsAtSameLevel.matches(deserializedSymbolsAtSameLevel), ::generateErrorMessage)

        (originalSymbolsAtSameLevel.typeParameterSymbols zip deserializedSymbolsAtSameLevel.typeParameterSymbols).forEach { (originalSymbol, deserializedSymbol) ->
            typeParameterSymbolsForRemapping[deserializedSymbol] = originalSymbol
        }

        (originalSymbolsAtSameLevel.valueParameterSymbols zip deserializedSymbolsAtSameLevel.valueParameterSymbols).forEach { (originalSymbol, deserializedSymbol) ->
            valueParameterSymbolsForRemapping[deserializedSymbol] = originalSymbol
        }
    }

    return TypeAndValueParameterSymbolsToRemap(typeParameterSymbolsForRemapping, valueParameterSymbolsForRemapping)
}

private class DeserializedDeclarationLeakagePreventingSymbolRemapper(
    wrapped: SymbolRemapper
) : SymbolRemapper, DeclaredSymbolRemapper by wrapped, ReferencedSymbolRemapper by Impl(wrapped) {

    private class Impl(private val wrapped: ReferencedSymbolRemapper) : ReferencedSymbolRemapper {

        override fun getReferencedClass(symbol: IrClassSymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedClass) { IrClassSymbolImpl(signature = it) }

        override fun getReferencedProperty(symbol: IrPropertySymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedProperty) { IrPropertySymbolImpl(signature = it) }

        override fun getReferencedConstructor(symbol: IrConstructorSymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedConstructor) { IrConstructorSymbolImpl(signature = it) }

        override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedEnumEntry) { IrEnumEntrySymbolImpl(signature = it) }

        override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedSimpleFunction) { IrSimpleFunctionSymbolImpl(signature = it) }

        override fun getReferencedField(symbol: IrFieldSymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedField) { IrFieldSymbolImpl(signature = it) }

        override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedTypeParameter) { IrTypeParameterSymbolImpl(signature = it) }

        override fun getReferencedTypeAlias(symbol: IrTypeAliasSymbol) =
            remapReferencedSymbolOrCreateNew(symbol, wrapped::getReferencedTypeAlias) { IrTypeAliasSymbolImpl(signature = it) }

        private inline fun <R : IrSymbol, O : R> remapReferencedSymbolOrCreateNew(
            originalSymbol: O,
            remap: (O) -> R,
            createNew: (IdSignature) -> O,
        ): R {
            val remappedSymbol = remap(originalSymbol)
            return runIf(originalSymbol === remappedSymbol && originalSymbol.isBound) {
                originalSymbol.signature?.let(createNew)
            } ?: remappedSymbol
        }

        override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol) =
            wrapped.getReferencedLocalDelegatedProperty(symbol)

        override fun getReferencedVariable(symbol: IrVariableSymbol) =
            wrapped.getReferencedVariable(symbol)

        override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol) =
            wrapped.getReferencedReturnableBlock(symbol)

        override fun getReferencedReturnTarget(symbol: IrReturnTargetSymbol): IrReturnTargetSymbol =
            wrapped.getReferencedReturnTarget(symbol)

        override fun getReferencedValueParameter(symbol: IrValueParameterSymbol) =
            wrapped.getReferencedValueParameter(symbol)

        override fun getReferencedScript(symbol: IrScriptSymbol) =
            wrapped.getReferencedScript(symbol)
    }
}
