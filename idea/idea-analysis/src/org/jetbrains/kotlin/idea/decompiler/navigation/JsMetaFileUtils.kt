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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.isDefaultPackageMetafile
import org.jetbrains.kotlin.serialization.js.isPackageClassFqName

public object JsMetaFileUtils {

    public fun getPackageFqName(file: VirtualFile): FqName = getPackageFqName(getRelativeToRootPath(file))

    public fun getClassFqName(file: VirtualFile): FqName = getClassFqName(getRelativeToRootPath(file))

    public fun getClassId(file: VirtualFile): ClassId = getClassId(getRelativeToRootPath(file))

    public fun isPackageHeader(file: VirtualFile): Boolean = isPackageHeader(getRelativeToRootPath(file))

    public fun getModuleDirectory(file: VirtualFile): VirtualFile =
        getRoot(file).findChild(getModuleName(getRelativeToRootPath(file)))!!

    private fun getRelativeToRootPath(file: VirtualFile): String = VfsUtilCore.getRelativePath(file, getRoot(file))!!

    private fun getClassFqName(relPath: String): FqName {
        val pathToFile = relPath.substringAfter(VfsUtilCore.VFS_SEPARATOR_CHAR)
        return FqName(pathToFile.substringBeforeLast('.').replace(VfsUtilCore.VFS_SEPARATOR_CHAR, '.'))
    }

    private fun getClassId(relPath: String): ClassId {
        val classFqName = getClassFqName(relPath)
        val packageFqName = getPackageFqName(relPath)

        val name = classFqName.shortName().asString().substringBeforeLast('.')
        return ClassId(packageFqName, FqName(name), false)
    }

    private fun getPackageFqName(relPath: String): FqName {
        val pathToFile = relPath.substringAfter(VfsUtilCore.VFS_SEPARATOR_CHAR)
        if (isDefaultPackageMetafile(pathToFile)) return FqName.ROOT

        val name = pathToFile.substringBeforeLast(VfsUtilCore.VFS_SEPARATOR_CHAR)
        return FqName(name.replace(VfsUtilCore.VFS_SEPARATOR_CHAR, '.'))
    }

    private fun getModuleName(relPath: String): String = relPath.substringBefore(VfsUtilCore.VFS_SEPARATOR_CHAR)

    private fun isPackageHeader(relPath: String): Boolean {
        val classFqName = JsMetaFileUtils.getClassFqName(relPath)
        return classFqName.isPackageClassFqName()
    }

    private fun getRoot(file: VirtualFile): VirtualFile = if (file.getParent() == null) file else getRoot(file.getParent())
}

