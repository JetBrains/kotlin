/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedDeclarationsProcessor
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedDeclarationsSupport
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

abstract class KotlinIrLinker(
    private val currentModule: ModuleDescriptor?,
    val messageLogger: IrMessageLogger,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val exportedDependencies: List<ModuleDescriptor>,
    val symbolProcessor: IrSymbolDeserializer.(IrSymbol, IdSignature) -> IrSymbol = { s, _ -> s },
) : IrDeserializer, FileLocalAwareLinker {

    // Kotlin-MPP related data. Consider some refactoring
    val expectUniqIdToActualUniqId = mutableMapOf<IdSignature, IdSignature>()
    val topLevelActualUniqItToDeserializer = mutableMapOf<IdSignature, IrModuleDeserializer>()
    internal val expectSymbols = mutableMapOf<IdSignature, IrSymbol>()
    internal val actualSymbols = mutableMapOf<IdSignature, IrSymbol>()

    val modulesWithReachableTopLevels = mutableSetOf<IrModuleDeserializer>()

    protected val deserializersForModules = mutableMapOf<String, IrModuleDeserializer>()

    abstract val fakeOverrideBuilder: FakeOverrideBuilder

    abstract val translationPluginContext: TranslationPluginContext?

    private val triedToDeserializeDeclarationForSymbol = mutableSetOf<IrSymbol>()

    private lateinit var linkerExtensions: Collection<IrDeserializer.IrLinkerExtension>

    protected open val unlinkedDeclarationsSupport: UnlinkedDeclarationsSupport = UnlinkedDeclarationsSupport.DISABLED
    protected open val userVisibleIrModulesSupport: UserVisibleIrModulesSupport = UserVisibleIrModulesSupport.DEFAULT

    fun handleSignatureIdNotFoundInModuleWithDependencies(
        idSignature: IdSignature,
        moduleDeserializer: IrModuleDeserializer
    ): IrModuleDeserializer {
        if (unlinkedDeclarationsSupport.allowUnboundSymbols) {
            return object : IrModuleDeserializer(null, KotlinAbiVersion.CURRENT) {
                override fun contains(idSig: IdSignature): Boolean = false

                override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
                    return referenceDeserializedSymbol(symbolTable, null, symbolKind, idSig)
                }

                override val moduleFragment: IrModuleFragment
                    get() = TODO("Not yet implemented")
                override val moduleDependencies: Collection<IrModuleDeserializer> get() = emptyList()
                override val kind: IrModuleDeserializerKind
                    get() = TODO("Not yet implemented")
            }
        } else {
            throw SignatureIdNotFoundInModuleWithDependencies(
                idSignature = idSignature,
                problemModuleDeserializer = moduleDeserializer,
                allModuleDeserializers = deserializersForModules.values,
                userVisibleIrModulesSupport = userVisibleIrModulesSupport
            ).raiseIssue(messageLogger)
        }
    }

    fun resolveModuleDeserializer(module: ModuleDescriptor, idSignature: IdSignature?): IrModuleDeserializer {
        return deserializersForModules[module.name.asString()]
            ?: throw NoDeserializerForModule(module.name, idSignature).raiseIssue(messageLogger)
    }

    protected abstract fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer

    protected abstract fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean

    private fun deserializeAllReachableTopLevels() {
        while (modulesWithReachableTopLevels.isNotEmpty()) {
            val moduleDeserializer = modulesWithReachableTopLevels.first()
            modulesWithReachableTopLevels.remove(moduleDeserializer)

            moduleDeserializer.deserializeReachableDeclarations()
        }
    }

    private fun findDeserializedDeclarationForSymbol(symbol: IrSymbol): DeclarationDescriptor? {

        if (symbol in triedToDeserializeDeclarationForSymbol) {
            return null
        }
        triedToDeserializeDeclarationForSymbol.add(symbol)

        if (!symbol.hasDescriptor) return null
        val descriptor = symbol.descriptor

        val moduleDeserializer = resolveModuleDeserializer(descriptor.module, symbol.signature)

        moduleDeserializer.declareIrSymbol(symbol)

        deserializeAllReachableTopLevels()
        if (!symbol.isBound) return null
        return descriptor
    }

    protected open fun platformSpecificSymbol(symbol: IrSymbol): Boolean = false

    private fun tryResolveCustomDeclaration(symbol: IrSymbol): IrDeclaration? {
        if (!symbol.hasDescriptor) return null

        val descriptor = symbol.descriptor

        if (descriptor is CallableMemberDescriptor) {
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                // skip fake overrides
                return null
            }
        }

        return translationPluginContext?.let { ctx ->
            linkerExtensions.firstNotNullOfOrNull {
                it.resolveSymbol(symbol, ctx)
            }?.also {
                require(symbol.owner == it)
            }
        }
    }

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? =
        deserializeOrResolveDeclaration(symbol, false)

    protected fun deserializeOrResolveDeclaration(symbol: IrSymbol, allowSymbolsWithoutSignaturesFromOtherModule: Boolean): IrDeclaration? {
        if (!allowSymbolsWithoutSignaturesFromOtherModule) {
            if (!symbol.isPublicApi && symbol.hasDescriptor && !platformSpecificSymbol(symbol) &&
                symbol.descriptor.module !== currentModule
            ) return null
        }

        if (!symbol.isBound) {
            try {
                findDeserializedDeclarationForSymbol(symbol)
                    ?: tryResolveCustomDeclaration(symbol)
                    ?: return null
            } catch (e: IrSymbolTypeMismatchException) {
                if (!unlinkedDeclarationsSupport.allowUnboundSymbols) {
                    throw SymbolTypeMismatch(e, deserializersForModules.values, userVisibleIrModulesSupport).raiseIssue(messageLogger)
                }
            }
        }

        // TODO: we do have serializations for those, but let's just create a stub for now.
        if (!symbol.isBound && (symbol.descriptor.isExpectMember || symbol.descriptor.containingDeclaration?.isExpectMember == true))
            return null

        if (!symbol.isBound) return null

        //assert(symbol.isBound) {
        //    "getDeclaration: symbol $symbol is unbound, descriptor = ${symbol.descriptor}, signature = ${symbol.signature}"
        //}

        return symbol.owner as IrDeclaration
    }

    override fun tryReferencingSimpleFunctionByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrSimpleFunctionSymbol? {
        if (idSignature.isPubliclyVisible) return null
        val file = parent.file
        val moduleDescriptor = file.packageFragmentDescriptor.containingDeclaration
        return resolveModuleDeserializer(moduleDescriptor, null).referenceSimpleFunctionByLocalSignature(file, idSignature)
    }

    override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? {
        if (idSignature.isPubliclyVisible) return null
        val file = parent.file
        val moduleDescriptor = file.packageFragmentDescriptor.containingDeclaration
        return resolveModuleDeserializer(moduleDescriptor, null).referencePropertyByLocalSignature(file, idSignature)
    }

    protected open fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer =
        CurrentModuleDeserializer(moduleFragment, dependencies)

    override fun init(moduleFragment: IrModuleFragment?, extensions: Collection<IrDeserializer.IrLinkerExtension>) {
        linkerExtensions = extensions
        if (moduleFragment != null) {
            val currentModuleDependencies = moduleFragment.descriptor.allDependencyModules.map {
                resolveModuleDeserializer(it, null)
            }
            val currentModuleDeserializer = createCurrentModuleDeserializer(moduleFragment, currentModuleDependencies)
            deserializersForModules[moduleFragment.name.asString()] =
                maybeWrapWithBuiltInAndInit(moduleFragment.descriptor, currentModuleDeserializer)
        }
        deserializersForModules.values.forEach { it.init() }
    }


    private fun markUnlinkedClassifiers(): Set<IrClassifierSymbol> {
        if (!unlinkedDeclarationsSupport.allowUnboundSymbols) return emptySet()
        val entries = fakeOverrideBuilder.fakeOverrideCandidates
        val result = mutableSetOf<IrClassifierSymbol>()

        fun IrType.isUnlinked(visited: MutableSet<IrClassifierSymbol>): Boolean {
            val simpleType = this as? IrSimpleType ?: return this !is IrErrorType

            val classifier = simpleType.classifier

            if (!classifier.isBound) {
                return true
            }

            if (classifier in result) {
                return true
            }

            if (!visited.add(classifier)) return false

            val superTypes = when (val decl = classifier.owner) {
                is IrClass -> decl.superTypes
                is IrTypeParameter -> decl.superTypes
                else -> emptyList()
            }

            if (superTypes.any { it.isUnlinked(visited) }) {
                result.add(classifier)
                return true
            }

            for (ta in simpleType.arguments) {
                if (ta is IrTypeProjection) {
                    val projected = ta.type
                    if (projected.isUnlinked(visited)) {
                        return true
                    }
                }
            }

            return false
        }

        fun IrClass.isUnlinked(visited: MutableSet<IrClassifierSymbol>): Boolean {
            if (symbol in result) return true
            for (s in superTypes) {
                if (s.isUnlinked(visited)) {
                    result.add(symbol)
                    return true
                }
            }
            return false
        }

        val toRemove = mutableListOf<IrClass>()

        for (e in entries) {
            val klass = e.key
            if (klass.isUnlinked(mutableSetOf())) {
                toRemove.add(klass)
            }
        }

        toRemove.forEach { entries.remove(it) }

        return result
    }

    private fun applyToModules(transformer: IrElementTransformerVoid) {
        deserializersForModules.values.forEach { it.moduleFragment.transformChildrenVoid(transformer) }
    }

    override fun postProcess() {
        finalizeExpectActualLinker()

        val unlinkedClassifiers = markUnlinkedClassifiers()

        fakeOverrideBuilder.provideFakeOverrides()
        triedToDeserializeDeclarationForSymbol.clear()

        unlinkedDeclarationsSupport.whenUnboundSymbolsAllowed { unlinkedMarkerTypeHandler ->
            val t = UnlinkedDeclarationsProcessor(builtIns, unlinkedClassifiers, unlinkedMarkerTypeHandler, messageLogger)
            t.addLinkageErrorIntoUnlinkedClasses()

            applyToModules(t.signatureTransformer())
            applyToModules(t.usageTransformer())
        }

        // TODO: fix IrPluginContext to make it not produce additional external reference
        // symbolTable.noUnboundLeft("unbound after fake overrides:")
    }

    fun handleExpectActualMapping(idSig: IdSignature, rawSymbol: IrSymbol): IrSymbol {

        // Actual signature
        if (idSig in expectUniqIdToActualUniqId.values) {
            actualSymbols[idSig] = rawSymbol
        }

        // Expect signature
        expectUniqIdToActualUniqId[idSig]?.let { actualSig ->
            assert(idSig.run { IdSignature.Flags.IS_EXPECT.test() })

            val referencingSymbol = wrapInDelegatedSymbol(rawSymbol)

            expectSymbols[idSig] = referencingSymbol

            // Trigger actual symbol deserialization
            topLevelActualUniqItToDeserializer[actualSig]?.let { moduleDeserializer -> // Not null if top-level
                val actualSymbol = actualSymbols[actualSig]
                // Check if
                if (actualSymbol == null || !actualSymbol.isBound) {
                    moduleDeserializer.addModuleReachableTopLevel(actualSig)
                }
            }

            return referencingSymbol
        }

        return rawSymbol
    }

    private fun topLevelKindToSymbolKind(kind: IrDeserializer.TopLevelSymbolKind): BinarySymbolData.SymbolKind {
        return when (kind) {
            IrDeserializer.TopLevelSymbolKind.CLASS_SYMBOL -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
            IrDeserializer.TopLevelSymbolKind.PROPERTY_SYMBOL -> BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
            IrDeserializer.TopLevelSymbolKind.FUNCTION_SYMBOL -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
            IrDeserializer.TopLevelSymbolKind.TYPEALIAS_SYMBOL -> BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
        }
    }

    override fun resolveBySignatureInModule(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleName: Name): IrSymbol {
        val moduleDeserializer =
            deserializersForModules.entries.find { it.key == moduleName.asString() }?.value
                ?: error("No module for name '$moduleName' found")
        assert(signature == signature.topLevelSignature()) { "Signature '$signature' has to be top level" }
        if (signature !in moduleDeserializer) error("No signature $signature in module $moduleName")
        return moduleDeserializer.deserializeIrSymbol(signature, topLevelKindToSymbolKind(kind)).also {
            deserializeAllReachableTopLevels()
        }
    }

    private inline fun <
            reified D : IrDeclaration,
            reified ES : IrDelegatingSymbol<AS, D, *>,
            reified AS : IrBindableSymbol<*, D>
            > finalizeExpectActual(expectSymbol: ES, actualSymbol: IrSymbol, noinline actualizer: (e: D, a: D) -> Unit) {
        require(actualSymbol is AS)
        val expectDeclaration = expectSymbol.owner
        val actualDeclaration = actualSymbol.owner
        actualizer(expectDeclaration, actualDeclaration)
        expectSymbol.delegate = actualSymbol
    }

    private fun actualizeIrFunction(e: IrFunction, a: IrFunction) {
        for (i in 0 until e.valueParameters.size) {
            val evp = e.valueParameters[i]
            val avp = a.valueParameters[i]
            val defaultValue = evp.defaultValue
            if (avp.defaultValue == null && defaultValue != null) {
                avp.defaultValue = defaultValue.patchDeclarationParents(a)
                evp.defaultValue = null
            }
        }
    }

    // The issue here is that an expect can not trigger its actual deserialization by reachability
    // because the expect can not see the actual higher in the module dependency dag.
    // So we force deserialization of actuals for all deserialized expect symbols here.
    private fun finalizeExpectActualLinker() {
        // All actuals have been deserialized, retarget delegating symbols from expects to actuals.
        expectUniqIdToActualUniqId.forEach {
            val expectSymbol = expectSymbols[it.key]
            val actualSymbol = actualSymbols[it.value]
            if (expectSymbol != null && actualSymbol != null) {
                when (expectSymbol) {
                    is IrDelegatingClassSymbolImpl ->
                        finalizeExpectActual(expectSymbol, actualSymbol) { _, _ -> }
                    is IrDelegatingEnumEntrySymbolImpl ->
                        finalizeExpectActual(expectSymbol, actualSymbol) { _, _ -> }
                    is IrDelegatingSimpleFunctionSymbolImpl ->
                        finalizeExpectActual(expectSymbol, actualSymbol) { e, a -> actualizeIrFunction(e, a) }
                    is IrDelegatingConstructorSymbolImpl ->
                        finalizeExpectActual(expectSymbol, actualSymbol) { e, a -> actualizeIrFunction(e, a) }
                    is IrDelegatingPropertySymbolImpl ->
                        finalizeExpectActual(expectSymbol, actualSymbol) { _, _ -> }
                    else -> error("Unexpected expect symbol kind during actualization: $expectSymbol")
                }
            }
        }
    }

    fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        kotlinLibrary: KotlinLibrary?,
        deserializationStrategy: (String) -> DeserializationStrategy = { DeserializationStrategy.ONLY_REFERENCED },
        _moduleName: String? = null
    ): IrModuleFragment {
        assert(kotlinLibrary != null || _moduleName != null) { "Either library or explicit name have to be provided $moduleDescriptor" }
        val moduleName = kotlinLibrary?.uniqueName?.let { "<$it>" } ?: _moduleName!!
        assert(moduleDescriptor.name.asString() == moduleName) {
            "${moduleDescriptor.name.asString()} != $moduleName"
        }
        val deserializerForModule = deserializersForModules.getOrPut(moduleName) {
            maybeWrapWithBuiltInAndInit(moduleDescriptor, createModuleDeserializer(moduleDescriptor, kotlinLibrary, deserializationStrategy))
        }
        // The IrModule and its IrFiles have been created during module initialization.
        return deserializerForModule.moduleFragment
    }

    protected open fun maybeWrapWithBuiltInAndInit(
        moduleDescriptor: ModuleDescriptor,
        moduleDeserializer: IrModuleDeserializer
    ): IrModuleDeserializer =
        if (isBuiltInModule(moduleDescriptor)) IrModuleDeserializerWithBuiltIns(builtIns, moduleDeserializer)
        else moduleDeserializer

    fun deserializeIrModuleHeader(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?, moduleName: String): IrModuleFragment {
        // TODO: consider skip deserializing explicitly exported declarations for libraries.
        // Now it's not valid because of all dependencies that must be computed.
        val deserializationStrategy: (String) -> DeserializationStrategy =
            if (exportedDependencies.contains(moduleDescriptor)) {
                { DeserializationStrategy.ALL }
            } else {
                { DeserializationStrategy.EXPLICITLY_EXPORTED }
            }
        return deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, deserializationStrategy, moduleName)
    }

    fun deserializeFullModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, { DeserializationStrategy.ALL })

    fun deserializeOnlyHeaderModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, { DeserializationStrategy.ONLY_DECLARATION_HEADERS })

    fun deserializeHeadersWithInlineBodies(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, { DeserializationStrategy.WITH_INLINE_BODIES })

    fun deserializeDirtyFiles(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary, dirtyFiles: Collection<String>): IrModuleFragment {
        return deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, {
            if (it in dirtyFiles) DeserializationStrategy.ALL
            else DeserializationStrategy.WITH_INLINE_BODIES
        })
    }
}

enum class DeserializationStrategy(
    val needBodies: Boolean,
    val explicitlyExported: Boolean,
    val theWholeWorld: Boolean,
    val inlineBodies: Boolean
) {
    ONLY_REFERENCED(true, false, false, true),
    ALL(true, true, true, true),
    EXPLICITLY_EXPORTED(true, true, false, true),
    ONLY_DECLARATION_HEADERS(false, false, false, false),
    WITH_INLINE_BODIES(false, false, false, true)
}
