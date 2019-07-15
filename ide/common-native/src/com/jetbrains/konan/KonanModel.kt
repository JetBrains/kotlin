/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import org.jetbrains.kotlin.gradle.KonanArtifactModel
import java.io.Serializable

interface KonanModel : Serializable {
    val artifacts: List<KonanArtifactModel>
    val buildTaskPath: String
    val cleanTaskPath: String
    val kotlinNativeHome: String

    companion object {
        const val NO_TASK_PATH = ""
        const val NO_KOTLIN_NATIVE_HOME = ""
    }
}
