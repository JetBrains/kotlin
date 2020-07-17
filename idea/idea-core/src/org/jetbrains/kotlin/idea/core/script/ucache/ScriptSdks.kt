/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.ucache

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile

class ScriptSdks(
    private val sdks: Map<SdkId, Sdk?>,
    val nonIndexedClassRoots: Set<VirtualFile>,
    val nonIndexedSourceRoots: Set<VirtualFile>
) {
    fun rebuild(project: Project, remove: Sdk?): ScriptSdks {
        val builder = ScriptSdksBuilder(project, remove = remove)
        sdks.keys.forEach { id ->
            builder.addSdk(id)
        }
        return builder.build()
    }

    val first: Sdk? = sdks.values.firstOrNull()

    operator fun get(sdkId: SdkId) = sdks[sdkId]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScriptSdks

        if (nonIndexedClassRoots != other.nonIndexedClassRoots) return false
        if (nonIndexedSourceRoots != other.nonIndexedSourceRoots) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nonIndexedClassRoots.hashCode()
        result = 31 * result + nonIndexedSourceRoots.hashCode()
        return result
    }
}