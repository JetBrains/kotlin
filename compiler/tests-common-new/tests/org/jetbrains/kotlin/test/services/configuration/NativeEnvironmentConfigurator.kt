/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.DUMP_KLIB_SYNTHETIC_ACCESSORS
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

class NativeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object : KlibBasedEnvironmentConfiguratorUtils {
        private const val TEST_PROPERTY_NATIVE_HOME = "kotlin.internal.native.test.nativeHome"
        private const val TEST_PROPERTY_TEST_TARGET = "kotlin.internal.native.test.target"

        fun getRuntimePathsForModule(module: TestModule, testServices: TestServices): List<String> {
            return testServices.nativeEnvironmentConfigurator.getRuntimePathsForModule(module)
        }
    }

    private val nativeHome: String by lazy {
        System.getProperty(TEST_PROPERTY_NATIVE_HOME)
            ?: testServices.assertions.fail {
                "No '$TEST_PROPERTY_NATIVE_HOME' provided. Are you sure the test are executed within :native:native.tests?"
            }
    }

    private val defaultNativeTarget: KonanTarget by lazy {
        val userDefinedTarget = System.getProperty(TEST_PROPERTY_TEST_TARGET)
        if (userDefinedTarget != null) {
            HostManager().targets[userDefinedTarget]
                ?: testServices.assertions.fail { "Unsupported target name specified in '$TEST_PROPERTY_TEST_TARGET': $userDefinedTarget" }
        } else {
            HostManager.host
        }
    }

    fun getNativeTarget(module: TestModule): KonanTarget {
        val testDefinedTarget = module.directives[NativeEnvironmentConfigurationDirectives.WITH_FIXED_TARGET].firstOrNull()
        return if (testDefinedTarget != null) {
            HostManager().targets[testDefinedTarget]
                ?: testServices.assertions.fail { "Unsupported target name specified in '${NativeEnvironmentConfigurationDirectives.WITH_FIXED_TARGET}': $testDefinedTarget" }
        } else {
            defaultNativeTarget
        }
    }

    fun distributionKlibPath(): File = File(nativeHome, "klib")

    fun getRuntimePathsForModule(module: TestModule): List<String> {
        val result = mutableListOf<String>()

        if (ConfigurationDirectives.WITH_STDLIB in module.directives) {
            result += distributionKlibPath().resolve("common").resolve("stdlib").absolutePath
        }

        if (NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS in module.directives) {
            val nativeTarget = getNativeTarget(module)

            // Diagnostic tests are agnostic of native target, so host is enforced to be a target.
            distributionKlibPath().resolve("platform").resolve(nativeTarget.name).listFiles()?.forEach {
                result += it.absolutePath
            }
        }

        testServices.runtimeClasspathProviders
            .flatMap { it.runtimeClassPaths(module) }
            .mapTo(result) { it.absolutePath }

        return result
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(NativeEnvironmentConfigurationDirectives, KlibBasedCompilerTestDirectives)

    override fun configureCompilerConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule,
    ) {
        if (!module.targetPlatform(testServices).isNative()) return

        val registeredDirectives = module.directives
        if (DUMP_KLIB_SYNTHETIC_ACCESSORS in registeredDirectives) {
            configuration.put(
                KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR,
                testServices.getOrCreateTempDirectory("synthetic-accessors").absolutePath
            )
        }
    }
}

val TestServices.nativeEnvironmentConfigurator: NativeEnvironmentConfigurator
    get() = environmentConfigurators.firstIsInstanceOrNull<NativeEnvironmentConfigurator>()
        ?: assertions.fail { "No registered ${NativeEnvironmentConfigurator::class.java.simpleName}" }
