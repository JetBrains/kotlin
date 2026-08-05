/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.LoadedKlibs
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isJsStdlib
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name.special
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class ModulesStructure(
    val mainModulePath: String,
    val compilerConfiguration: CompilerConfiguration,
    val klibs: LoadedKlibs,
) {
    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings

    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
    private var runtimeModule: ModuleDescriptorImpl? = null

    // TODO: these are roughly equivalent to KlibResolvedModuleDescriptorsFactoryImpl. Refactor me.
    val descriptors: Map<KotlinLibrary, ModuleDescriptor>
        field = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()

    init {
        val descriptors = klibs.all.map { getModuleDescriptorImpl(it) }
        val friendDescriptors = klibs.friends.mapTo(mutableSetOf(), ::getModuleDescriptorImpl)
        descriptors.forEach { descriptor ->
            descriptor.setDependencies(descriptors, friendDescriptors)
        }
    }

    private fun getModuleDescriptorImpl(current: KotlinLibrary): ModuleDescriptorImpl {
        if (current in descriptors) {
            return descriptors.getValue(current)
        }

        val isBuiltIns = current.isJsStdlib || current.isWasmStdlib

        val moduleName = special("<${current.uniqueName}>")
        val moduleOrigin = DeserializedKlibModuleOrigin(current)
        val md = if (runtimeModule?.builtIns != null)
            JsFactories.DefaultDescriptorFactory.createDescriptor(moduleName, storageManager, runtimeModule!!.builtIns, moduleOrigin)
        else
            JsFactories.DefaultDescriptorFactory.createDescriptorAndNewBuiltIns(moduleName, storageManager, moduleOrigin)

        if (isBuiltIns) runtimeModule = md
        descriptors[current] = md

        return md
    }

    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptor =
        getModuleDescriptorImpl(current)
}
