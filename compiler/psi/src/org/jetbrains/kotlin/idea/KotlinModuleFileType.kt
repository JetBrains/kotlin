/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.idea

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.vfs.VirtualFile
import java.util.function.Supplier
import javax.swing.Icon

class KotlinModuleFileType private constructor() : FileType {
    private val myIcon = NotNullLazyValue.lazy<Icon>(Supplier { KotlinIconProviderService.Companion.instance.getFileIcon() })

    override fun getName(): String {
        return EXTENSION
    }

    override fun getDescription(): String {
        return "Kotlin module info: contains package part mappings"
    }

    override fun getDefaultExtension(): String {
        return EXTENSION
    }

    override fun getIcon(): Icon {
        return myIcon.getValue()
    }

    override fun isBinary(): Boolean {
        return true
    }

    override fun isReadOnly(): Boolean {
        return true
    }

    override fun getCharset(file: VirtualFile, content: ByteArray): String? {
        return null
    }

    companion object {
        const val EXTENSION: String = "kotlin_module"
        val INSTANCE: KotlinModuleFileType = KotlinModuleFileType()
    }
}
