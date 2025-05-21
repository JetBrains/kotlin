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
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.isPublicApi
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

abstract class KotlinIrLinker(
    private val currentModule: ModuleDescriptor?,
    val messageCollector: MessageCollector,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val exportedDependencies: List<ModuleDescriptor>,
    val deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
) : IrDeserializer, FileLocalAwareLinker {
    val irInterner = IrInterningService()

    /**
     * This is the queue of modules containing top-level declarations to be deserialized. This is
     * the third-layer queue on top of [BasicIrModuleDeserializer.ModuleDeserializationState.filesWithPendingTopLevels] and
     * [FileDeserializationState.reachableTopLevels].
     *
     * A module can be enqueued using [BasicIrModuleDeserializer.ModuleDeserializationState.enqueueFile].
     * TODO: provide a more clear API for enqueueing IR modules, KT-73819
     *
     * The deserialization happens on invocation of [deserializeAllReachableTopLevels]. This in its turn
     * invokes [IrModuleDeserializer.deserializeReachableDeclarations] for each scheduled module.
     *
     * Note: A module is removed from the queue after all top-level declarations scheduled for
     * deserialization in that module have been actually deserialized. Later the module can be enqueued
     * once again to deserialize other top-level declaration(s). This process can be repeated multiple times.
     */
    val modulesWithReachableTopLevels = linkedSetOf<IrModuleDeserializer>()

    protected val deserializersForModules = linkedMapOf<String, IrModuleDeserializer>()

    abstract val fakeOverrideBuilder: IrLinkerFakeOverrideProvider

    private val triedToDeserializeDeclarationForSymbol = hashSetOf<IrSymbol>()

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
                ).raiseIssue(messageCollector)
        }
    }

    fun resolveModuleDeserializer(module: ModuleDescriptor, idSignature: IdSignature?): IrModuleDeserializer {
        return deserializersForModules[module.name.asString()]
            ?: NoDeserializerForModule(module.name, idSignature).raiseIssue(messageCollector)
    }

    protected abstract fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer

    protected abstract fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean

    /**
     * Run deserialization of top-level declarations previously scheduled for deserialization in the current [KotlinIrLinker].
     */
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
                findDeserializedDeclarationForSymbol(symbol) ?: return null
            } catch (e: IrSymbolTypeMismatchException) {
                SymbolTypeMismatch(e, deserializersForModules.values, userVisibleIrModulesSupport).raiseIssue(messageCollector)
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

    override fun init(moduleFragment: IrModuleFragment?) {
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

    /**
     * KLIBs don't contain enough information for initializing the correct shape for [IrFunctionReference]s and [IrPropertyReference]s.
     *
     * For example, consider the following code:
     *
     * ```kotlin
     * class C {
     *     fun foo() {}
     * }
     *
     * fun bar() {}
     * ```
     *
     * Function references `C::foo` and `::bar` will both be serialized (and deserialized) as having the following shape:
     * ```
     * dispatch_receiver = null
     * extension_receiver = null
     * value_argument = []
     * ```
     *
     * However, `C::foo` has unbound dispatch receiver, while `::bar` doesn't have any dispatch receiver.
     * To be able to adopt the new value parameter API ([KT-71850](https://youtrack.jetbrains.com/issue/KT-71850)),
     * we have to be able to distinguish these two situations, because for `C::foo` the target function's [IrFunction.parameters]
     * will be [[dispatch receiver]], while for `::bar` the target function's [IrFunction.parameters] will be an empty list,
     * and [IrFunctionReference.arguments] must always match the target function's [IrFunction.parameters] list.
     *
     * The same applies to [IrPropertyReference].
     *
     * Because existing KLIBs already don't contain enough information for setting the correct shape, the following hack is used:
     * After linking we visit callable references and update their shape from the linked target function/property.
     *
     * See [KT-71849](https://youtrack.jetbrains.com/issue/KT-71849).
     */
    private fun fixCallableReferences() {
        deserializersForModules.values.forEach {
            it.moduleFragment.acceptChildrenVoid(
                object : IrVisitorVoid() {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitFunctionReference(expression: IrFunctionReference) {
                        if (expression.symbol.isBound) {
                            expression.initializeTargetShapeFromSymbol()
                        }
                        super.visitFunctionReference(expression)
                    }

                    override fun visitPropertyReference(expression: IrPropertyReference) {
                        if (expression.symbol.isBound) {
                            expression.initializeTargetShapeFromSymbol()
                        }
                        super.visitPropertyReference(expression)
                    }
                }
            )
        }
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
            partialLinkageSupport.generateStubsAndPatchUsages(symbolTable)

            fixCallableReferences()
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

        val moduleFragment = deserializersForModules.getOrPut(moduleName) {
            maybeWrapWithBuiltInAndInit(moduleDescriptor, createModuleDeserializer(moduleDescriptor, kotlinLibrary, deserializationStrategy))
        }.moduleFragment

        moduleFragment.kotlinLibrary = kotlinLibrary

        // The IrModule and its IrFiles have been created during module initialization.
        return moduleFragment
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

/** This is an auxiliary attribute that is used to store [KotlinLibrary] instance for deserialized [IrModuleFragment]. */
var IrModuleFragment.kotlinLibrary: KotlinLibrary? by irAttribute(copyByDefault = false)
    private set
