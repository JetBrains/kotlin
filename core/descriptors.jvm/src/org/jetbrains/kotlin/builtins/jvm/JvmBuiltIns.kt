/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.sure

class JvmBuiltIns @JvmOverloads constructor(
    storageManager: StorageManager,
    loadBuiltInsFromCurrentClassLoader: Boolean = true
) : KotlinBuiltIns(storageManager) {
    // Module containing JDK classes or having them among dependencies
    private var ownerModuleDescriptor: ModuleDescriptor? = null
    private var isAdditionalBuiltInsFeatureSupported: Boolean = true

    fun initialize(moduleDescriptor: ModuleDescriptor, isAdditionalBuiltInsFeatureSupported: Boolean) {
        assert(ownerModuleDescriptor == null) { "JvmBuiltins repeated initialization" }
        this.ownerModuleDescriptor = moduleDescriptor
        this.isAdditionalBuiltInsFeatureSupported = isAdditionalBuiltInsFeatureSupported
    }

    val settings: JvmBuiltInsSettings by storageManager.createLazyValue {
        JvmBuiltInsSettings(
            builtInsModule, storageManager,
            { ownerModuleDescriptor.sure { "JvmBuiltins has not been initialized properly" } },
            {
                ownerModuleDescriptor.sure { "JvmBuiltins has not been initialized properly" }
                isAdditionalBuiltInsFeatureSupported
            }
        )
    }

    init {
        if (loadBuiltInsFromCurrentClassLoader) {
            createBuiltInsModule()
        }
    }

    override fun getPlatformDependentDeclarationFilter(): PlatformDependentDeclarationFilter = settings

    override fun getAdditionalClassPartsProvider(): AdditionalClassPartsProvider = settings

    override fun getClassDescriptorFactories() =
        super.getClassDescriptorFactories() + JvmBuiltInClassDescriptorFactory(storageManager, builtInsModule)
}
