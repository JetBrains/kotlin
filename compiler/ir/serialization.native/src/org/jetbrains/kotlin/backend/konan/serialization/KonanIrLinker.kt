/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.linkage.issues.UserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.packageFragmentDescriptor
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.psi2ir.lazy.DeclarationStubGenerator

@OptIn(ObsoleteDescriptorBasedAPI::class)
class KonanIrLinker(
    private val currentModule: ModuleDescriptor,
    messageCollector: MessageCollector,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable,
    friendModules: Map<String, Collection<String>>,
    private val forwardModuleDescriptor: ModuleDescriptor?,
    private val stubGenerator: DeclarationStubGenerator,
    private val cInteropModuleDeserializerFactory: CInteropModuleDeserializerFactory,
    exportedDependencies: List<ModuleDescriptor>,
    override val partialLinkageSupport: PartialLinkageSupportForLinker,
    private val libraryBeingCached: PartialCacheInfo?,
    override val userVisibleIrModulesSupport: UserVisibleIrModulesSupport,
    externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) : KotlinIrLinker(currentModule, messageCollector, builtIns, symbolTable, exportedDependencies) {

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean = moduleDescriptor.isNativeStdlib()

    private val forwardDeclarationDeserializer = forwardModuleDescriptor?.let {
        KonanForwardDeclarationModuleDeserializer(it, this, stubGenerator)
    }

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
        linker = this,
        symbolTable = symbolTable,
        mangler = KonanManglerIr,
        typeSystem = IrTypeSystemContextImpl(builtIns),
        friendModules = friendModules,
        partialLinkageSupport = partialLinkageSupport,
        platformSpecificClassFilter = KonanFakeOverrideClassFilter,
        externalOverridabilityConditions = externalOverridabilityConditions,
    )

    val moduleDeserializers = mutableMapOf<ModuleDescriptor, KonanPartialModuleDeserializer>()
    val klibToModuleDeserializerMap = mutableMapOf<KotlinLibrary, KonanPartialModuleDeserializer>()

    val inlineFunctionFilesTracker = InlineFunctionFilesTracker()

    override fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ) = when {
        moduleDescriptor === forwardModuleDescriptor -> {
            forwardDeclarationDeserializer ?: error("forward declaration deserializer expected")
        }
        klib == null -> {
            error("Expecting kotlin library for $moduleDescriptor")
        }
        klib.isCInteropLibrary() -> {
            cInteropModuleDeserializerFactory.createIrModuleDeserializer(
                moduleDescriptor,
                klib,
                listOfNotNull(forwardDeclarationDeserializer),
            )
        }
        else -> {
            val deserializationStrategy = when {
                klib == libraryBeingCached?.klib -> libraryBeingCached.strategy
                else -> CacheDeserializationStrategy.WholeModule
            }
            KonanPartialModuleDeserializer(
                this, moduleDescriptor, klib, stubGenerator, strategyResolver, deserializationStrategy
            ).also {
                moduleDeserializers[moduleDescriptor] = it
                klibToModuleDeserializerMap[klib] = it
            }
        }
    }

    override fun postProcess(inOrAfterLinkageStep: Boolean) {
        stubGenerator.unboundSymbolGeneration = true
        super.postProcess(inOrAfterLinkageStep)
    }

    override fun getFileOf(declaration: IrDeclaration): IrFile {
        val packageFragment = declaration.getPackageFragment()
        return packageFragment as? IrFile
            ?: inlineFunctionFilesTracker.getOrNull(packageFragment as IrExternalPackageFragment)
            ?: error("Unknown external package fragment: ${packageFragment.packageFragmentDescriptor}")
    }

    private val String.isForwardDeclarationModuleName: Boolean get() = this == KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME.asString()

    val modules: Map<String, IrModuleFragment>
        get() = mutableMapOf<String, IrModuleFragment>().apply {
            deserializersForModules
                .filter { !it.key.isForwardDeclarationModuleName && it.value.moduleDescriptor !== currentModule }
                .forEach {
                    val klib = it.value.klib as? KotlinLibrary ?: error("Expected to be KotlinLibrary (${it.key})")
                    this[klib.libraryName] = it.value.moduleFragment
                }
        }
}