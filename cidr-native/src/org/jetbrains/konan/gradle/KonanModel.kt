/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.io.File
import java.io.Serializable

interface KonanModel : Serializable {
    val artifacts: List<KonanModelArtifact>
    val buildTaskPath: String
    val cleanTaskPath: String
    val kotlinNativeHome: String

    companion object {
        const val NO_TASK_PATH = ""
        const val NO_KOTLIN_NATIVE_HOME = ""
    }
}

interface KonanModelArtifact : Serializable {
    val name: String
    val type: CompilerOutputKind
    val targetPlatform: String
    val file: File
    val buildTaskPath: String
    val isTests: Boolean
}
