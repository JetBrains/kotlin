/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.fileTypes.FileTypeManager
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION

class KotlinNativeApplicationComponent : ApplicationComponent {

    override fun getComponentName(): String = "KotlinNativeApplicationComponent"

    override fun initComponent() {
        FileTypeManager.getInstance().associateExtension(ArchiveFileType.INSTANCE, KLIB_FILE_EXTENSION)

        // TODO: Move this to Kotlin/Native plugin for CLion and AppCode (see KT-26717):
//        val extensionPoint = Extensions.getRootArea().getExtensionPoint(TipAndTrickBean.EP_NAME)
//        for (name in arrayOf("Kotlin.html", "Kotlin_project.html", "Kotlin_mix.html", "Kotlin_Java_convert.html")) {
//            TipAndTrickBean.findByFileName(name)?.let {
//                extensionPoint.unregisterExtension(it)
//            }
//        }
    }
}
