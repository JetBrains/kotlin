/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.configuration

import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.test.services.service
import java.io.File

class AnalysisApiJvmEnvironmentConfigurator(testServices: TestServices) : JvmEnvironmentConfigurator(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = super.additionalServices + listOf(
            service(::CompiledLibraryProvider),
        )

    override fun convertDependencyToFileList(dependency: DependencyDescription): List<File> {
        val friendModule = testServices.dependencyProvider.getTestModule(dependency.moduleName)
        testServices.compiledLibraryProvider.getCompiledLibrary(friendModule.name)?.artifact?.let {
            return listOf(it.toFile())
        }
        return super.convertDependencyToFileList(dependency)
    }
}