/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.loader.DefaultKlibLibraryProvider
import org.jetbrains.kotlin.library.loader.KlibLibraryProvider
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class NativeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object : KlibBasedEnvironmentConfiguratorUtils {
        private const val TEST_PROPERTY_NATIVE_HOME = "kotlin.internal.native.test.nativeHome"
        private const val TEST_PROPERTY_TEST_TARGET = "kotlin.internal.native.test.target"

        /**
         * WARNING: Please consider using [NativeEnvironmentConfigurator.getRuntimeLibraryProviders] instead.
         *
         * Unlike [NativeEnvironmentConfigurator.getRuntimeLibraryProviders], which returns the list of library providers,
         * that are capable of locating and properly loading libraries, this function returns just the list of raw library paths.
         *
         * That could be not enough in certain cases. For example, in the case of loading the libraries from the Kotlin/Native distribution,
         * which all need to be marked with [Klib.isFromKotlinNativeDistribution] flag that is checked by the Kotlin/Native backend later.
         */
        fun getRuntimePathsForModule(module: TestModule, testServices: TestServices): List<String> {
            return testServices.nativeEnvironmentConfigurator.getRuntimeLibraryProviders(module).flatMap { it.getLibraryPaths() }
        }
    }

    private val nativeHome: File by lazy {
        System.getProperty(TEST_PROPERTY_NATIVE_HOME)?.let(::File)
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

    fun getRuntimeLibraryProviders(module: TestModule): List<KlibLibraryProvider> {
        val nativeDistributionProvider = KlibNativeDistributionLibraryProvider(nativeHome) {
            runIf(ConfigurationDirectives.WITH_STDLIB in module.directives) {
                withStdlib()
            }

            runIf(NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS in module.directives) {
                withPlatformLibs(getNativeTarget(module))
            }
        }

        val additionalLibrariesProvider: KlibLibraryProvider? = testServices.runtimeClasspathProviders
            .asSequence()
            .flatMap { it.runtimeClassPaths(module) }
            .map { it.absolutePath }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { additionalPaths -> DefaultKlibLibraryProvider(additionalPaths) }

        return listOfNotNull(nativeDistributionProvider, additionalLibrariesProvider)
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(NativeEnvironmentConfigurationDirectives, KlibBasedCompilerTestDirectives)
}

val TestServices.nativeEnvironmentConfigurator: NativeEnvironmentConfigurator
    get() = environmentConfigurators.firstIsInstanceOrNull<NativeEnvironmentConfigurator>()
        ?: assertions.fail { "No registered ${NativeEnvironmentConfigurator::class.java.simpleName}" }
