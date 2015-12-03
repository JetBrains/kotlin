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

package org.jetbrains.kotlin.idea.decompiler.builtIns

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.BuiltInsSerializedResourcePaths
import org.jetbrains.kotlin.idea.KotlinIcons

public object KotlinBuiltInClassFileType : FileType {
    override fun getName() = "kotlin_class"

    override fun getDescription() = "Kotlin builtin class"

    override fun getDefaultExtension() = BuiltInsSerializedResourcePaths.CLASS_METADATA_FILE_EXTENSION

    override fun getIcon() = KotlinIcons.CLASS

    override fun isBinary() = true

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray) = null
}

public object KotlinBuiltInPackageFileType : FileType {
    override fun getName() = "kotlin_package"

    override fun getDescription() = "Kotlin builtin package"

    override fun getDefaultExtension() = BuiltInsSerializedResourcePaths.PACKAGE_FILE_EXTENSION

    override fun getIcon() = KotlinIcons.FILE

    override fun isBinary() = true

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray) = null
}