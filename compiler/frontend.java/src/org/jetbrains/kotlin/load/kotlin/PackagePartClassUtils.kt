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

package org.jetbrains.kotlin.load.kotlin

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

object PackagePartClassUtils {
    @JvmStatic fun getPathHashCode(file: VirtualFile): Int =
            file.path.toLowerCase().hashCode()

    private val PART_CLASS_NAME_SUFFIX = "Kt"

    private @JvmStatic fun decapitalizeAsJavaClassName(str: String): String =
            // NB use Locale.ENGLISH so that build is locale-independent.
            // See Javadoc on java.lang.String.toUpperCase() for more details.
            when {
                Character.isJavaIdentifierStart(str[0]) -> str.substring(0, 1).toLowerCase(Locale.ENGLISH) + str.substring(1)
                str[0] == '_' -> str.substring(1)
                else -> str
            }

    @TestOnly
    @JvmStatic fun getDefaultPartFqName(facadeClassFqName: FqName, file: VirtualFile): FqName =
            getPackagePartFqName(facadeClassFqName.parent(), file.name)

    @JvmStatic fun getPackagePartFqName(packageFqName: FqName, fileName: String): FqName {
        val partClassName = getFilePartShortName(fileName)
        return packageFqName.child(Name.identifier(partClassName))
    }

    @JvmStatic fun getFilesWithCallables(files: Collection<KtFile>): List<KtFile> =
            files.filter { it.hasTopLevelCallables() }

    @JvmStatic fun getFilePartShortName(fileName: String): String =
            NameUtils.getPackagePartClassNamePrefix(FileUtil.getNameWithoutExtension(fileName)) + PART_CLASS_NAME_SUFFIX

    @JvmStatic fun getFileNameByFacadeName(facadeClassName: String): String? {
        if (!facadeClassName.endsWith(PART_CLASS_NAME_SUFFIX)) return null
        val baseName = facadeClassName.substring(0, facadeClassName.length - PART_CLASS_NAME_SUFFIX.length)
        if (baseName == "_") return null
        return "${decapitalizeAsJavaClassName(baseName)}.${KotlinFileType.EXTENSION}"
    }
}
