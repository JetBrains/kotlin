/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.StorageManager

interface PackageViewDescriptorFactory {
    fun compute(
        module: ModuleDescriptorImpl,
        fqName: FqName,
        storageManager: StorageManager
    ): PackageViewDescriptor

    object Default: PackageViewDescriptorFactory {
        override fun compute(module: ModuleDescriptorImpl, fqName: FqName, storageManager: StorageManager): PackageViewDescriptor {
            return LazyPackageViewDescriptorImpl(module, fqName, storageManager)
        }
    }

    companion object {
        val CAPABILITY = ModuleCapability<PackageViewDescriptorFactory>("PackageViewDescriptorFactory")
    }
}
