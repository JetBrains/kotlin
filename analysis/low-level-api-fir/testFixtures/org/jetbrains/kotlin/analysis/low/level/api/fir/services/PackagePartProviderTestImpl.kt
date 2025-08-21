/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.moduleStructure

internal class PackagePartProviderTestImpl(
    private val testServices: TestServices,
) : KotlinPackagePartProviderFactory {
    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        val providers = testServices.moduleStructure.modules
            .map { module -> testServices.compilerConfigurationProvider.getPackagePartProviderFactory(module)(scope) }

        return when (providers.size) {
            0 -> PackagePartProvider.Empty
            1 -> providers.single()
            else -> CompositePackagePartProvider(providers)
        }
    }
}

private class CompositePackagePartProvider(private val providers: List<PackageAndMetadataPartProvider>) : PackageAndMetadataPartProvider {
    override fun findPackageParts(packageFqName: String): List<String> {
        return providers.flatMapTo(mutableSetOf()) { it.findPackageParts(packageFqName) }.toList()
    }

    override fun computePackageSetWithNonClassDeclarations(): Set<String>? {
        return providers.flatMapTo(mutableSetOf()) { it.computePackageSetWithNonClassDeclarations() ?: return null }
    }

    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
        return providers.flatMapTo(mutableSetOf()) { it.getAnnotationsOnBinaryModule(moduleName) }.toList()
    }

    override fun getAllOptionalAnnotationClasses(): List<ClassData> {
        return providers.flatMapTo(mutableSetOf()) { it.getAllOptionalAnnotationClasses() }.toList()
    }

    override fun mayHaveOptionalAnnotationClasses(): Boolean {
        return providers.any { it.mayHaveOptionalAnnotationClasses() }
    }

    override fun findMetadataPackageParts(packageFqName: String): List<String> {
        return providers.flatMapTo(mutableListOf()) { it.findMetadataPackageParts(packageFqName) }
    }
}