/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.PackagePartProviderFactory
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.moduleStructure

internal class PackagePartProviderTestImpl(
    private val testServices: TestServices,
) : PackagePartProviderFactory() {
    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        val providers = testServices.moduleStructure.modules.map { module ->
            testServices.compilerConfigurationProvider.getPackagePartProviderFactory(module)(scope)
        }
        return object : PackagePartProvider {
            override fun findPackageParts(packageFqName: String): List<String> {
                return providers.flatMapTo(mutableSetOf()) { it.findPackageParts(packageFqName) }.toList()
            }

            override fun computePackageSetWithNonClassDeclarations(): Set<String> =
                providers.flatMapTo(mutableSetOf()) { it.computePackageSetWithNonClassDeclarations() }

            override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
                return providers.flatMapTo(mutableSetOf()) { it.getAnnotationsOnBinaryModule(moduleName) }.toList()
            }

            override fun getAllOptionalAnnotationClasses(): List<ClassData> {
                return providers.flatMapTo(mutableSetOf()) { it.getAllOptionalAnnotationClasses() }.toList()
            }

            override fun mayHaveOptionalAnnotationClasses(): Boolean = providers.any { it.mayHaveOptionalAnnotationClasses() }
        }
    }
}
