/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.AbstractBundle
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import org.jetbrains.annotations.PropertyKey

val KONAN_MODEL_KEY = Key.create(KonanModel::class.java, ProjectKeys.MODULE.processingWeight + 1)

private const val PATH_TO_BUNDLE: String = "KonanBundle"

object KonanBundle : AbstractBundle(PATH_TO_BUNDLE) {
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}


object XmlRunConfiguration {
    const val attributeDirectory = "WORKING_DIR"
    const val attributeParameters = "PROGRAM_PARAMS"
    const val attributePassParent = "PASS_PARENT_ENVS_2"
}


object XmlKonanWorkspace {
    const val nodeAllExecutables: String = "executables"
    const val nodeExecutable = "executable"
    const val attributeKonanHome = "KOTLIN_NATIVE_HOME"
}


object XmlExecutable {
    const val attributeTargetType = "TARGET_TYPE"
    const val attributeTargetName = "TARGET"
    const val attributeBinaryName = "BINARY"
}


object XmlExecutionTarget {
    const val nodeName = "target"

    const val attributeIsDebug = "DEBUG"
    const val attributeFileName = "FILE"
    const val attributeGradleTask = "GRADLE_TASK"
}
