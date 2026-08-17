/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

interface KlibBasedEnvironmentConfigurator {
    fun getKlibArtifactSimpleName(testServices: TestServices, moduleName: String): String {
        val testName = testServices.testInfo.methodName.removePrefix("test").decapitalizeAsciiOnly()
        val outputFileSuffix = if (moduleName == ModuleStructureExtractor.DEFAULT_MODULE_NAME) "" else "-$moduleName"
        return testName + outputFileSuffix
    }

    /**
     * The location of the generated KLIB artifact (as a directory).
     */
    fun getKlibArtifactDir(testServices: TestServices, moduleName: String): File {
        return getKlibOutputDir(testServices).resolve(getKlibArtifactSimpleName(testServices, moduleName))
    }

    /**
     * The location of the generated KLIB artifact (as a ZIP archive).
     */
    fun getKlibArtifactFile(testServices: TestServices, moduleName: String): File {
        return getKlibArtifactDir(testServices, moduleName).run { resolveSibling("$name.klib") }
    }

    fun getKlibOutputDir(testServices: TestServices): File {
        return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(OUTPUT_KLIB_DIR_NAME)
    }

    companion object {
        private const val OUTPUT_KLIB_DIR_NAME = "outputKlibDir"
    }
}

val TestServices.klibEnvironmentConfigurator: KlibBasedEnvironmentConfigurator
    get() = environmentConfigurators.firstIsInstanceOrNull<KlibBasedEnvironmentConfigurator>()
        ?: error("No registered ${KlibBasedEnvironmentConfigurator::class.java.simpleName}")
