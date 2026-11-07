/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassIdToJavaClass
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import java.io.IOException

internal fun readJavaModuleInfo(file: VirtualFile, classesByClassId: ClassIdToJavaClass): JavaModuleInfo? {
    val contents = try {
        file.contentsToByteArray()
    } catch (_: IOException) {
        return null
    }
    return JavaModuleInfo.read(contents, file.canonicalPath, classesByClassId)
}
