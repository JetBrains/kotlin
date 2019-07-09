/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.resolve.getAnnotationsOnContainingJsModule
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.getJvmModuleNameForDeserializedDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver

class CliModuleAnnotationsResolver : ModuleAnnotationsResolver {
    private val packagePartProviders = mutableListOf<PackagePartProvider>()

    fun addPackagePartProvider(packagePartProvider: PackagePartProvider) {
        packagePartProviders += packagePartProvider
    }

    override fun getAnnotationsOnContainingModule(descriptor: DeclarationDescriptor): List<ClassId> {
        getAnnotationsOnContainingJsModule(descriptor)?.let { return it }

        val moduleName = getJvmModuleNameForDeserializedDescriptor(descriptor) ?: return emptyList()
        return packagePartProviders.flatMap { it.getAnnotationsOnBinaryModule(moduleName) }
    }
}
