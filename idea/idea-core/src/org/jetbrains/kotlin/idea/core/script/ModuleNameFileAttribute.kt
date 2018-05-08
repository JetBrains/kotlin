/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import kotlin.reflect.KProperty

var VirtualFile.scriptRelatedModuleName: String? by ScratchModuleNameProperty()

private val moduleNameAttribute = FileAttribute("kotlin-script-moduleName", 1, false)
private class ScratchModuleNameProperty {

    operator fun getValue(file: VirtualFile, property: KProperty<*>): String? {
        if (file !is VirtualFileWithId) return null

        return moduleNameAttribute.readAttributeBytes(file)?.let { String(it) }
    }

    operator fun setValue(file: VirtualFile, property: KProperty<*>, newValue: String?) {
        if (file !is VirtualFileWithId) return

        if (newValue != null) {
            moduleNameAttribute.writeAttributeBytes(file, newValue.toByteArray())
        }
    }
}