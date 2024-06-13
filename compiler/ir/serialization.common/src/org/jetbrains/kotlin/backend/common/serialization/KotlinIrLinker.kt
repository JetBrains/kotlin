/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.linkage.issues.*
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
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
    val irInterner = IrInterningService()

    val modulesWithReachableTopLevels = linkedSetOf<IrModuleDeserializer>()

    protected val deserializersForModules = linkedMapOf<String, IrModuleDeserializer>()

    abstract val fakeOverrideBuilder: IrLinkerFakeOverrideProvider

    abstract val translationPluginContext: TranslationPluginContext?

    private val triedToDeserializeDeclarationForSymbol = hashSetOf<IrSymbol>()

    private lateinit var linkerExtensions: Collection<IrDeserializer.IrLinkerExtension>

    open val partialLinkageSupport: PartialLinkageSupportForLinker get() = PartialLinkageSupportForLinker.DISABLED

    open val returnUnboundSymbolsIfSignatureNotFound: Boolean
        get() = partialLinkageSupport.isEnabled

    protected open val userVisibleIrModulesSupport: UserVisibleIrModulesSupport get() = UserVisibleIrModulesSupport.DEFAULT

    fun deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(
        idSignature: IdSignature,
        symbolKind: BinarySymbolData.SymbolKind,
        moduleDeserializer: IrModuleDeserializer
    ): IrSymbol {
        val topLevelSignature: IdSignature = idSignature.topLevelSignature()

        // Note: The top-level symbol might be gone in newer version of dependency KLIB. Then the KLIB that was compiled against
        // the older version of dependency KLIB will still have a reference to non-existing symbol. And the linker will have to
        // handle such situation appropriately. See KT-41378.
        val actualModuleDeserializer: IrModuleDeserializer? = moduleDeserializer.findModuleDeserializerForTopLevelId(topLevelSignature)

        // Note: It might happen that the top-level symbol still exists in KLIB, but nested symbol has been removed.
        // Then the `actualModuleDeserializer` will be non-null, but `actualModuleDeserializer.tryDeserializeIrSymbol()` call
        // might return null (like KonanInteropModuleDeserializer does) or non-null unbound symbol (like JsModuleDeserializer does).
        val symbol: IrSymbol? = actualModuleDeserializer?.tryDeserializeIrSymbol(idSignature, symbolKind)

        return symbol ?: run {
            if (returnUnboundSymbolsIfSignatureNotFound)
                referenceDeserializedSymbol(symbolTable, null, symbolKind, idSignature)
            else
                SignatureIdNotFoundInModuleWithDependencies(
                    idSignature = idSignature,
                    problemModuleDeserializer = moduleDeserializer,
                    allModuleDeserializers = deserializersForModules.values,
                    userVisibleIrModulesSupport = userVisibleIrModulesSupport
                ).raiseIssue(messageLogger)
        }
    }

    fun resolveModuleDeserializer(module: ModuleDescriptor, idSignature: IdSignature?): IrModuleDeserializer {
        return deserializersForModules[module.name.asString()]
            ?: NoDeserializerForModule(module.name, idSignature).raiseIssue(messageLogger)
    }

    protected abstract fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer

    protected abstract fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean

    fun deserializeAllReachableTopLevels() {
        while (modulesWithReachableTopLevels.isNotEmpty()) {
            val moduleDeserializer = modulesWithReachableTopLevels.first()
            modulesWithReachableTopLevels.remove(moduleDeserializer)

            moduleDeserializer.deserializeReachableDeclarations()
        }
    }

    private fun findDeserializedDeclarationForSymbol(symbol: IrSymbol): DeclarationDescriptor? {
        if (!triedToDeserializeDeclarationForSymbol.add(symbol)) return null
        val descriptor = if (symbol.hasDescriptor) symbol.descriptor else return null

        val moduleDeserializer = resolveModuleDeserializer(descriptor.module, symbol.signature)
        moduleDeserializer.declareIrSymbol(symbol)

        deserializeAllReachableTopLevels()

        return if (symbol.isBound) descriptor else null
    }

    protected open fun platformSpecificSymbol(symbol: IrSymbol): Boolean = false

    private fun tryResolveCustomDeclaration(symbol: IrSymbol): IrDeclaration? {
        val descriptor = if (symbol.hasDescriptor) symbol.descriptor else return null
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
                SymbolTypeMismatch(e, deserializersForModules.values, userVisibleIrModulesSupport).raiseIssue(messageLogger)
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

    open fun getFileOf(declaration: IrDeclaration): IrFile = declaration.file

    override fun tryReferencingSimpleFunctionByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrSimpleFunctionSymbol? {
        if (idSignature.isPubliclyVisible) return null
        val file = getFileOf(parent)
        val moduleDescriptor = file.moduleDescriptor
        return resolveModuleDeserializer(moduleDescriptor, null).referenceSimpleFunctionByLocalSignature(file, idSignature)
    }

    override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? {
        if (idSignature.isPubliclyVisible) return null
        val file = getFileOf(parent)
        val moduleDescriptor = file.moduleDescriptor
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

    fun clear() {
        irInterner.reset()
    }

    override fun postProcess(inOrAfterLinkageStep: Boolean) {
        if (inOrAfterLinkageStep) {
            // We have to exclude classifiers with unbound symbols in supertypes and in type parameter upper bounds from F.O. generation
            // to avoid failing with `Symbol for <signature> is unbound` error or generating fake overrides with incorrect signatures.
            partialLinkageSupport.exploreClassifiers(fakeOverrideBuilder)
        }

        // Fake override generator creates new IR declarations. This may have effect of binding for certain symbols.
        fakeOverrideBuilder.provideFakeOverrides()
        triedToDeserializeDeclarationForSymbol.clear()

        if (inOrAfterLinkageStep) {
            // Finally, generate stubs for the remaining unbound symbols and patch every usage of any unbound symbol inside the IR tree.
            partialLinkageSupport.generateStubsAndPatchUsages(symbolTable) {
                deserializersForModules.values.asSequence().map { it.moduleFragment }
            }
        }
        // TODO: fix IrPluginContext to make it not produce additional external reference
        // symbolTable.noUnboundLeft("unbound after fake overrides:")
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
        return moduleDeserializer.deserializeIrSymbolOrFail(signature, topLevelKindToSymbolKind(kind)).also {
            deserializeAllReachableTopLevels()
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
        if (isBuiltInModule(moduleDescriptor)) IrModuleDeserializerWithBuiltIns(builtIns, moduleDeserializer) else moduleDeserializer

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
    val onDemand: Boolean,
    val needBodies: Boolean,
    val explicitlyExported: Boolean,
    val theWholeWorld: Boolean,
    val inlineBodies: Boolean
) {
    ON_DEMAND(true, false, false, false, false),
    ONLY_REFERENCED(false, true, false, false, true),
    ALL(false, true, true, true, true),
    EXPLICITLY_EXPORTED(false, true, true, false, true),
    ONLY_DECLARATION_HEADERS(false, false, false, false, false),
    WITH_INLINE_BODIES(false, false, false, false, true)
}
