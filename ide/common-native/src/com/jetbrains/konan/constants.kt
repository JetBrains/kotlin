/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val PATH_TO_BUNDLE: String = "KonanBundle"

object KonanBundle : AbstractBundle(PATH_TO_BUNDLE) {
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}

object WorkspaceXML {
    const val projectComponentName = "KotlinMultiplatform"

    const val attributeKonanHome = "KOTLIN_NATIVE_HOME"

    object Executable {
        const val containerName = "executables"
        const val nodeName = "executable"

        const val attributeTargetType = "TARGET_TYPE"
        const val attributeTargetName = "TARGET"
        const val attributeBinaryName = "BINARY"
        const val attributeProjectPrefix = "PREFIX"
    }

    object Target {
        const val nodeName = "target"

        const val attributeIsDebug = "DEBUG"
        const val attributeFileName = "FILE"
        const val attributeGradleTask = "GRADLE_TASK"
    }

    object RunConfiguration {
        const val attributeDirectory = "WORKING_DIR"
        const val attributeParameters = "PROGRAM_PARAMS"
        const val attributePassParent = "PASS_PARENT_ENVS_2"

        const val attributeXcodeScheme = "XCODE_SCHEME"
    }

    object XCProject {
        const val nodeName = "xcodeproj"
        const val attributePath = "PATH"
    }
}
