/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

data class BinaryExecutable(
    val target: KonanTarget,
    val targetName: String,
    val execName: String,
    val projectPrefix: String,
    val isTest: Boolean,
    val variants: List<Variant>
) {
    val name: String
        get() = "$execName[$targetName]"

    sealed class Variant {
        abstract val gradleTask: String
        abstract val file: File
        abstract val params: RunParameters
        abstract val name: String

        data class Debug(
            override val gradleTask: String,
            override val file: File,
            override val params: RunParameters,
            override val name: String = "Debug"
        ) : Variant()

        data class Release(
            override val gradleTask: String,
            override val file: File,
            override val params: RunParameters,
            override val name: String = "Release"
        ) : Variant()
    }
}

data class RunParameters(
    val workingDirectory: String,
    val programParameters: String,
    val environmentVariables: Map<String, String>
)