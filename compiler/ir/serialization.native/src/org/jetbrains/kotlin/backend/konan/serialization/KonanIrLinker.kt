/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.PartialLinkageConfig
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.objcinterop.isObjCClass
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import java.nio.file.Path

class KonanIrLinker(
    private val currentModule: ModuleDescriptor,
    configuration: CompilerConfiguration,
    symbolTable: SymbolTable,
    friendModules: Map<String, Collection<String>>,
    private val forwardModuleDescriptor: ModuleDescriptor?,
    private val cInteropModuleDeserializerFactory: CInteropModuleDeserializerFactory,
    exportedDependencies: List<ModuleDescriptor>,
    partialLinkageConfig: PartialLinkageConfig,
    irDiagnosticReporter: IrDiagnosticReporter,
    private val libraryBeingCached: PartialCacheInfo?,
    externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) : KotlinIrLinker(currentModule, configuration, symbolTable, exportedDependencies) {
    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean {
        val klib = (moduleDescriptor.klibModuleOrigin as? DeserializedKlibModuleOrigin)?.library ?: return false
        return klib.isNativeStdlib
    }

    override val irMangler: KotlinMangler.IrMangler = KonanManglerIr

    override val partialLinkageSupport: PartialLinkageSupportForLinker = createPartialLinkageSupportForLinker(
        partialLinkageConfig = partialLinkageConfig,
        irFactory = symbolTable.irFactory,
        anyClass = anyClass,
        nothingClass = nothingClass,
        diagnosticReporter = irDiagnosticReporter,
    )

    private val globalDeclarationTable = KonanGlobalDeclarationTable(null)

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
        linker = this,
        symbolTable = symbolTable,
        mangler = irMangler,
        friendModules = friendModules,
        partialLinkageSupport = partialLinkageSupport,
        platformSpecificClassFilter = K1LazyFakeOverrideClassFilter,
        fakeOverrideDeclarationTable = KonanDeclarationTable(globalDeclarationTable),
        externalOverridabilityConditions = externalOverridabilityConditions,
        isMultipleInheritedImplementationsAllowed = {
            // Properties of ObjC protocols are serialized as final, along with their getters and setters.
            // In case of intersection override, the usual logic of IrLinkerFakeOverrideBuilderStrategy.postProcessGeneratedFakeOverride()
            // will raise AMBIGUOUS_NON_OVERRIDDEN_CALLABLE_MEMBER, since properties in protocols are non-abstract.
            // So such properties are allowed to form intersection overrides without a partial linkage error. Native backend will handle them correctly.
            // However, this predicate is much wider to cover also other similar usecases.
            it.parentAsClass.isObjCClass()
        },
    )

    val moduleDeserializers = mutableMapOf<IrModuleFragment, KonanPartialModuleDeserializer>()
    val klibToModuleDeserializerMap = mutableMapOf<KotlinLibrary, KonanPartialModuleDeserializer>()

    override fun createModuleDeserializer(
        moduleFragment: IrModuleFragment,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ) = when {
        moduleFragment.descriptor === forwardModuleDescriptor -> {
            KonanForwardDeclarationModuleDeserializer(moduleFragment, this)
        }
        klib == null -> {
            error("Expecting kotlin library for $moduleFragment")
        }
        klib.isCInteropLibrary() -> {
            cInteropModuleDeserializerFactory.createIrModuleDeserializer(
                moduleFragment,
                klib,
                this,
            )
        }
        else -> {
            val deserializationStrategy = when {
                klib == libraryBeingCached?.klib -> libraryBeingCached.strategy
                else -> CacheDeserializationStrategy.WholeModule
            }
            KonanPartialModuleDeserializer(
                this, moduleFragment, klib, strategyResolver, deserializationStrategy
            ).also {
                moduleDeserializers[moduleFragment] = it
                klibToModuleDeserializerMap[klib] = it
            }
        }
    }

    private val String.isForwardDeclarationModuleName: Boolean get() = this == FORWARD_DECLARATIONS_MODULE_NAME.asString()

    val modules: Map<Path, IrModuleFragment>
        get() = mutableMapOf<Path, IrModuleFragment>().apply {
            deserializersForModules
                .filter { !it.key.isForwardDeclarationModuleName && it.value.moduleFragment.descriptor !== currentModule }
                .forEach {
                    val klib = it.value.klib
                    this[klib.path] = it.value.moduleFragment
                }
        }
}
