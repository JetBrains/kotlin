/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.directives.model.ComposedRegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestModuleStructure
import java.io.File

class TestModuleStructureImpl(
    override val modules: List<TestModule>,
    override val originalTestDataFiles: List<File>
) : TestModuleStructure() {
    override val allDirectives: RegisteredDirectives = ComposedRegisteredDirectives(modules.map { it.directives })

    override fun toString(): String {
        return buildString {
            modules.forEach {
                appendLine(it)
                appendLine()
            }
        }
    }

    companion object {
        fun TargetPlatform.toArtifactKind(frontendKind: FrontendKind<out ResultingArtifact.FrontendOutput<*>>): BinaryKind<*> {
            if (frontendKind == FrontendKinds.ClassicAndFIR && this in JvmPlatforms.allJvmPlatforms) return ArtifactKinds.JvmFromK1AndK2
            return when (this) {
                in JvmPlatforms.allJvmPlatforms -> ArtifactKinds.Jvm
                in JsPlatforms.allJsPlatforms -> ArtifactKinds.Js
                in NativePlatforms.allNativePlatforms -> ArtifactKinds.Native
                in WasmPlatforms.allWasmPlatforms -> ArtifactKinds.Wasm
                else -> BinaryKind.NoArtifact
            }
        }
    }
}
