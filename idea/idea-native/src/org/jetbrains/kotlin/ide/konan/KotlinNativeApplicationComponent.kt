/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.fileTypes.FileTypeManager
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION

class KotlinNativeApplicationComponent : ApplicationComponent {

    override fun getComponentName(): String = "KotlinNativeApplicationComponent"

    override fun initComponent() {
        ApplicationManager.getApplication().runWriteAction {
            FileTypeManager.getInstance().associateExtension(ArchiveFileType.INSTANCE, KLIB_FILE_EXTENSION)
        }
    }
}
