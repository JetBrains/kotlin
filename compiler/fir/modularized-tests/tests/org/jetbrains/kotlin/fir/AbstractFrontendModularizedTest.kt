/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiJavaModule
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFrontendModularizedTest : AbstractModularizedTest() {

    fun configureLanguageVersionSettings(
        configuration: CompilerConfiguration,
        moduleData: ModuleData,
        languageVersion: LanguageVersion,
        configureFlags: MutableMap<AnalysisFlag<*>, Any?>.() -> Unit = {},
        configureFeatures: MutableMap<LanguageFeature, LanguageFeature.State>.() -> Unit = {}
    ) {
        val originalArguments = moduleData.arguments as? K2JVMCompilerArguments

        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion,
            originalArguments?.apiVersion?.let { ApiVersion.parse(it) } ?: ApiVersion.LATEST_STABLE,
            analysisFlags = buildMap {
                put(AnalysisFlags.optIn, moduleData.optInAnnotations + originalArguments?.optIn.orEmpty())
                if (originalArguments != null) {
                    put(AnalysisFlags.skipPrereleaseCheck, originalArguments.skipPrereleaseCheck)
                    put(JvmAnalysisFlags.jvmDefaultMode, JvmDefaultMode.fromStringOrNull(originalArguments.jvmDefault))
                }

                configureFlags()
            },
            specificFeatures = buildMap {
                configureFeatures()
            }
        )
    }

    fun createDefaultConfiguration(
        moduleData: ModuleData
    ): CompilerConfiguration {
        val configuration = KotlinTestUtils.newConfiguration()
        val originalArguments = moduleData.arguments as? K2JVMCompilerArguments

        moduleData.javaSourceRoots.forEach {
            configuration.addJavaSourceRoot(it.path, it.packagePrefix)
        }

        val isJava9Module = moduleData.javaSourceRoots.any { (file, packagePrefix) ->
            packagePrefix == null &&
                    (file.name == PsiJavaModule.MODULE_INFO_FILE ||
                            (file.isDirectory && file.listFiles()!!.any { it.name == PsiJavaModule.MODULE_INFO_FILE }))
        }

        for (rootPath in moduleData.classpath) {
            if (isJava9Module) {
                configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(rootPath))
            }
            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(rootPath))
        }

        val jdkHome =
            moduleData.modularJdkRoot
                ?: moduleData.jdkHome?.absoluteFile
                ?: originalArguments?.jdkHome?.fixPath()?.absoluteFile

        if (originalArguments != null) {
            configuration.put(JVMConfigurationKeys.NO_JDK, originalArguments.noJdk)

            for (modularRoot in originalArguments.javaModulePath.orEmpty()) {
                configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(modularRoot.fixPath()))
            }

            configuration.put(
                JVMConfigurationKeys.JVM_TARGET,
                originalArguments.jvmTargetIfSupported() ?: JvmTarget.DEFAULT
            )
        }
        configuration.putIfNotNull(JVMConfigurationKeys.JDK_HOME, jdkHome)
        configuration.configureJdkClasspathRoots()

        configuration.addAll(
            CLIConfigurationKeys.CONTENT_ROOTS,
            moduleData.sources
                .filter { it.extension == "kt" || it.isDirectory }
                .map { KotlinSourceRoot(it.absolutePath, isCommon = false, hmppModuleName = null) }
        )

        configuration.addAll(JVMConfigurationKeys.FRIEND_PATHS, moduleData.friendDirs.map { it.absolutePath })

        return configuration
    }
}
