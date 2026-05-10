/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.test.framework.services.TargetPlatformDirectives.TARGET_PLATFORM
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TargetPlatformProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.getMetadataTargetPlatformOrNull

object TargetPlatformDirectives : SimpleDirectivesContainer() {
    val TARGET_PLATFORM by enumDirective<TargetPlatformEnum>(
        "Declares target platform for current module"
    )
}

enum class TargetPlatformEnum(val targetPlatform: TargetPlatform) {
    Common(CommonPlatforms.defaultCommonPlatform),

    JVM(JvmPlatforms.unspecifiedJvmPlatform),
    JVM_1_6(JvmPlatforms.jvm6),
    JVM_1_8(JvmPlatforms.jvm8),

    JS(JsPlatforms.defaultJsPlatform),
    WasmWasi(WasmPlatforms.wasmWasi),
    Wasm(WasmPlatforms.wasmJs),
    WasmJs(WasmPlatforms.wasmJs),
    Native(NativePlatforms.unspecifiedNativePlatform)
}

/**
 * For a given [TestModule], this target platform provider has four possible sources to decide the target platform:
 *
 * 1. The [TARGET_PLATFORM] directive, which is Analysis API-specific and can be set on any module.
 * 2. The [METADATA_TARGET_PLATFORMS][org.jetbrains.kotlin.test.directives.ConfigurationDirectives.METADATA_TARGET_PLATFORMS] directive,
 *    which specifies the target platform for a *metadata* module. It has no effect on leaf modules unless
 *    [METADATA_ONLY_COMPILATION][org.jetbrains.kotlin.test.directives.ConfigurationDirectives.METADATA_ONLY_COMPILATION] is enabled by the
 *    test. This directive mainly supports metadata test data originating from the compiler side.
 * 3. The module name suffix, such as `*-common` or `*-jvm`. See [parseModulePlatformByName].
 * 4. The default target platform, provided by
 *    [DefaultsProvider.targetPlatform][org.jetbrains.kotlin.test.services.DefaultsProvider.targetPlatform].
 */
class TargetPlatformProviderForAnalysisApiTests(val testServices: TestServices) : TargetPlatformProvider() {
    override fun getTargetPlatform(module: TestModule): TargetPlatform {
        val explicitlyDeclaredPlatforms = module.directives[TARGET_PLATFORM]
        val platforms = explicitlyDeclaredPlatforms.map { it.targetPlatform }
        @OptIn(TestInfrastructureInternals::class)
        return when (platforms.size) {
            0 -> getTargetPlatformFromContext(module)
            1 -> platforms.single()
            else -> {
                if (TargetPlatformEnum.Common in explicitlyDeclaredPlatforms) {
                    testServices.assertions.fail { "You can't specify `Common` platform in combination with others" }
                }
                TargetPlatform(platforms.flatMapTo(mutableSetOf()) { it.componentPlatforms })
            }
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    private fun getTargetPlatformFromContext(module: TestModule): TargetPlatform {
        // For metadata compiler test data, `METADATA_TARGET_PLATFORMS` specifies the composite target platform. This takes priority over
        // the module name suffix because a common module like `lib-common` should get the explicitly specified platform (e.g., JS+WasmJs),
        // not the broadest possible platform.
        return getMetadataTargetPlatformOrNull(module, testServices)
            ?: parseModulePlatformByName(module.name)
            ?: testServices.defaultsProvider.targetPlatform
    }

    private fun parseModulePlatformByName(moduleName: String): TargetPlatform? {
        val nameSuffix = moduleName.substringAfterLast("-", "").uppercase()
        return when {
            nameSuffix == "COMMON" -> CommonPlatforms.defaultCommonPlatform
            nameSuffix == "JVM" -> JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): determine JvmTarget precisely
            nameSuffix == "JS" -> JsPlatforms.defaultJsPlatform
            nameSuffix == "WASM" -> WasmPlatforms.wasmJs
            nameSuffix == "NATIVE" -> NativePlatforms.unspecifiedNativePlatform
            // TODO(KT-76788): consider inferring proper target platform
            nameSuffix.isEmpty() -> null
            else -> null
        }
    }
}
