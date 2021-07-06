/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.KotlinIrLinkerInternalException
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

abstract class KotlinIrLinker(
    private val currentModule: ModuleDescriptor?,
    val messageLogger: IrMessageLogger,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val exportedDependencies: List<ModuleDescriptor>,
) : IrDeserializer, FileLocalAwareLinker {

    // Kotlin-MPP related data. Consider some refactoring
    val expectUniqIdToActualUniqId = mutableMapOf<IdSignature, IdSignature>()
    val topLevelActualUniqItToDeserializer = mutableMapOf<IdSignature, IrModuleDeserializer>()
    internal val expectSymbols = mutableMapOf<IdSignature, IrSymbol>()
    internal val actualSymbols = mutableMapOf<IdSignature, IrSymbol>()

    val modulesWithReachableTopLevels = mutableSetOf<IrModuleDeserializer>()

    // TODO: replace with Map<Name, IrModuleDeserializer>
    protected val deserializersForModules = mutableMapOf<ModuleDescriptor, IrModuleDeserializer>()

    abstract val fakeOverrideBuilder: FakeOverrideBuilder

    abstract val translationPluginContext: TranslationPluginContext?

    internal val triedToDeserializeDeclarationForSymbol = mutableSetOf<IrSymbol>()
    val deserializedSymbols = mutableSetOf<IrSymbol>()

    private lateinit var linkerExtensions: Collection<IrDeserializer.IrLinkerExtension>

    public open fun handleNoModuleDeserializerFound(idSignature: IdSignature, currentModule: ModuleDescriptor, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer {
        val message = buildString {
            append("Module ${currentModule.name} has reference $idSignature, unfortunately neither itself nor its dependencies ")
            dependencies.joinTo(this, "\n\t", "[\n\t", "\n]") { it.moduleDescriptor.name.asString() }
            append(" contain this declaration")
            append("\n")
            append("Please check that project configuration is correct and has required dependencies.")
        }
        messageLogger.report(IrMessageLogger.Severity.ERROR, message, null)

        throw KotlinIrLinkerInternalException
    }

    public open fun resolveModuleDeserializer(module: ModuleDescriptor, signature: IdSignature?): IrModuleDeserializer {
        return deserializersForModules[module] ?: run {
            val message = buildString {
                append("Could not load module ")
                append(module)
                signature?.let {
                    append("; It was an attempt to find deserializer for ")
                    append(it)
                }
            }
            messageLogger.report(IrMessageLogger.Severity.ERROR, message, null)

            throw KotlinIrLinkerInternalException
        }
    }

    protected abstract fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: IrLibrary?,
        strategy: DeserializationStrategy,
    ): IrModuleDeserializer

    protected abstract val functionalInterfaceFactory: IrAbstractFunctionFactory

    protected abstract fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean

    private fun deserializeAllReachableTopLevels() {
        while (modulesWithReachableTopLevels.isNotEmpty()) {
            val moduleDeserializer = modulesWithReachableTopLevels.first()
            modulesWithReachableTopLevels.remove(moduleDeserializer)

            moduleDeserializer.deserializeReachableDeclarations()
        }
    }

    private fun findDeserializedDeclarationForSymbol(symbol: IrSymbol): DeclarationDescriptor? {
        assert(symbol.isPublicApi || symbol.descriptor.module === currentModule || platformSpecificSymbol(symbol))

        if (symbol in triedToDeserializeDeclarationForSymbol || symbol in deserializedSymbols) {
            return null
        }
        triedToDeserializeDeclarationForSymbol.add(symbol)

        val descriptor = symbol.descriptor

        val moduleDeserializer = resolveModuleDeserializer(descriptor.module, symbol.signature)

//        moduleDeserializer.deserializeIrSymbol(signature, symbol.kind())
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

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {

        if (!symbol.isPublicApi) {
            val descriptor = symbol.descriptor
            if (!platformSpecificSymbol(symbol)) {
                if (descriptor.module !== currentModule) return null
            }
        }

        if (!symbol.isBound) {
            findDeserializedDeclarationForSymbol(symbol) ?: tryResolveCustomDeclaration(symbol) ?: return null
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
        if (idSignature.isPublic) return null
        val file = parent.file
        val moduleDescriptor = file.packageFragmentDescriptor.containingDeclaration
        return resolveModuleDeserializer(moduleDescriptor, null).referenceSimpleFunctionByLocalSignature(file, idSignature)
    }

    override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? {
        if (idSignature.isPublic) return null
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
            deserializersForModules[moduleFragment.descriptor] =
                maybeWrapWithBuiltInAndInit(moduleFragment.descriptor, currentModuleDeserializer)
        }
        deserializersForModules.values.forEach { it.init() }
    }

    override fun postProcess() {
        finalizeExpectActualLinker()
        fakeOverrideBuilder.provideFakeOverrides()
        triedToDeserializeDeclarationForSymbol.clear()
        deserializedSymbols.clear()

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
            deserializersForModules.entries.find { it.key.name == moduleName }?.value ?: error("No module for name '$moduleName' found")
        assert(signature == signature.topLevelSignature()) { "Signature '$signature' has to be top level" }
        if (signature !in moduleDeserializer) error("No signature $signature in module $moduleName")
        return moduleDeserializer.deserializeIrSymbol(signature, topLevelKindToSymbolKind(kind)).also {
            deserializeAllReachableTopLevels()
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
                        expectSymbol.delegate = actualSymbol as IrClassSymbol
                    is IrDelegatingEnumEntrySymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrEnumEntrySymbol
                    is IrDelegatingSimpleFunctionSymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrSimpleFunctionSymbol
                    is IrDelegatingConstructorSymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrConstructorSymbol
                    is IrDelegatingPropertySymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrPropertySymbol
                    else -> error("Unexpected expect symbol kind during actualization: $expectSymbol")
                }
            }
        }
    }

    fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        kotlinLibrary: KotlinLibrary?,
        deserializationStrategy: DeserializationStrategy = DeserializationStrategy.ONLY_REFERENCED
    ): IrModuleFragment {
        val deserializerForModule = deserializersForModules.getOrPut(moduleDescriptor) {
            maybeWrapWithBuiltInAndInit(moduleDescriptor, createModuleDeserializer(moduleDescriptor, kotlinLibrary, deserializationStrategy))
        }
        // The IrModule and its IrFiles have been created during module initialization.
        return deserializerForModule.moduleFragment
    }

    protected open fun maybeWrapWithBuiltInAndInit(
        moduleDescriptor: ModuleDescriptor,
        moduleDeserializer: IrModuleDeserializer
    ): IrModuleDeserializer =
        if (isBuiltInModule(moduleDescriptor)) IrModuleDeserializerWithBuiltIns(builtIns, functionalInterfaceFactory, moduleDeserializer)
        else moduleDeserializer

    fun deserializeIrModuleHeader(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment {
        // TODO: consider skip deserializing explicitly exported declarations for libraries.
        // Now it's not valid because of all dependencies that must be computed.
        val deserializationStrategy =
            if (exportedDependencies.contains(moduleDescriptor)) {
                DeserializationStrategy.ALL
            } else {
                DeserializationStrategy.EXPLICITLY_EXPORTED
            }
        return deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, deserializationStrategy)
    }

    fun deserializeFullModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, DeserializationStrategy.ALL)

    fun deserializeOnlyHeaderModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, DeserializationStrategy.ONLY_DECLARATION_HEADERS)

    fun deserializeHeadersWithInlineBodies(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, DeserializationStrategy.WITH_INLINE_BODIES)
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
