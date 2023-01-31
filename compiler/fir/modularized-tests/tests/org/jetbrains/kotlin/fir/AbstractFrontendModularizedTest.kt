/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractFrontendModularizedTest : AbstractModularizedTest() {
    fun createDefaultConfiguration(moduleData: ModuleData): CompilerConfiguration {
        val configuration = KotlinTestUtils.newConfiguration()
        moduleData.javaSourceRoots.forEach {
            configuration.addJavaSourceRoot(it.path, it.packagePrefix)
        }
        configuration.addJvmClasspathRoots(moduleData.classpath)
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            LanguageVersion.LATEST_STABLE,
            ApiVersion.LATEST_STABLE,
            analysisFlags = mutableMapOf(AnalysisFlags.optIn to moduleData.optInAnnotations)
        )
        configuration.configureJdkClasspathRoots()

        // in case of modular jdk only
        configuration.putIfNotNull(JVMConfigurationKeys.JDK_HOME, moduleData.modularJdkRoot)

        configuration.addAll(
            CLIConfigurationKeys.CONTENT_ROOTS,
            moduleData.sources.filter { it.extension == "kt" || it.isDirectory }.map { KotlinSourceRoot(it.absolutePath, false) })
        return configuration
    }
}