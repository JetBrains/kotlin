/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.util.KtTestUtil

class JKlibEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JvmEnvironmentConfigurationDirectives)

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val extractedJdkKind = JvmEnvironmentConfigurator.extractJdkKind(registeredDirectives)

        // Default programmatically to FULL_JDK_8 instead of MOCK_JDK so JRE classes are always present without stubs
        val jdkKind = if (extractedJdkKind == TestJdkKind.MOCK_JDK || extractedJdkKind == TestJdkKind.MODIFIED_MOCK_JDK) {
            TestJdkKind.FULL_JDK_8
        } else {
            extractedJdkKind
        }

        JvmEnvironmentConfigurator.getJdkHome(jdkKind)?.let { configuration.put(JVMConfigurationKeys.JDK_HOME, it) }

        when (jdkKind) {
            TestJdkKind.MOCK_JDK, TestJdkKind.MODIFIED_MOCK_JDK -> {
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
                configuration.addJvmClasspathRoot(KtTestUtil.findMockJdkRtJar())
            }
            else -> {
                JvmEnvironmentConfigurator.getJdkClasspathRoot(jdkKind)?.let { configuration.addJvmClasspathRoot(it) }
            }
        }

        configuration.configureJdkClasspathRoots()
    }
}
