/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.test.framework.services.TargetPlatformDirectives.TARGET_PLATFORM
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatformUnspecifiedTarget
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TargetPlatformProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.defaultsProvider

object TargetPlatformDirectives : SimpleDirectivesContainer() {
    val TARGET_PLATFORM by enumDirective<TargetPlatformEnum>(
        "Declares target platform for current module"
    )
}

enum class TargetPlatformEnum(val targetPlatform: TargetPlatform) {
    Common(
        TargetPlatform(
            setOf(
                JdkPlatform(JvmTarget.DEFAULT),
                JsPlatforms.DefaultSimpleJsPlatform,
                WasmPlatformWithTarget(WasmTarget.JS),
                WasmPlatformWithTarget(WasmTarget.WASI),
                NativePlatformUnspecifiedTarget
            )
        )
    ),

    JVM(JvmPlatforms.unspecifiedJvmPlatform),
    JVM_1_6(JvmPlatforms.jvm6),
    JVM_1_8(JvmPlatforms.jvm8),

    JS(JsPlatforms.defaultJsPlatform),
    WasmWasi(WasmPlatforms.wasmWasi),
    Wasm(WasmPlatforms.wasmJs),
    Native(NativePlatforms.unspecifiedNativePlatform)
}

class TargetPlatformProviderForAnalysisApiTests(val testServices: TestServices) : TargetPlatformProvider() {
    override fun getTargetPlatform(module: TestModule): TargetPlatform {
        val explicitlyDeclaredPlatforms = module.directives[TARGET_PLATFORM]
        val platforms = explicitlyDeclaredPlatforms.map { it.targetPlatform }
        @OptIn(TestInfrastructureInternals::class)
        return when (platforms.size) {
            0 -> parseModulePlatformByName(module.name) ?: testServices.defaultsProvider.targetPlatform
            1 -> platforms.single()
            else -> {
                if (TargetPlatformEnum.Common in explicitlyDeclaredPlatforms) {
                    testServices.assertions.fail { "You can't specify `Common` platform in combination with others" }
                }
                TargetPlatform(platforms.flatMapTo(mutableSetOf()) { it.componentPlatforms })
            }
        }
    }

    private fun parseModulePlatformByName(moduleName: String): TargetPlatform? {
        val nameSuffix = moduleName.substringAfterLast("-", "").uppercase()
        return when {
            nameSuffix == "COMMON" -> CommonPlatforms.defaultCommonPlatform
            nameSuffix == "JVM" -> JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): determine JvmTarget precisely
            nameSuffix == "JS" -> JsPlatforms.defaultJsPlatform
            nameSuffix == "WASM" -> WasmPlatforms.wasmJs
            nameSuffix == "NATIVE" -> NativePlatforms.unspecifiedNativePlatform
            nameSuffix.isEmpty() -> null // TODO(dsavvinov): this leads to 'null'-platform in ModuleDescriptor
            else -> throw IllegalStateException("Can't determine platform by name $nameSuffix")
        }
    }
}
