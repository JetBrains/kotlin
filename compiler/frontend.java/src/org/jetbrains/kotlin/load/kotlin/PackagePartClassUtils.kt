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
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtScript
import java.util.*

object PackagePartClassUtils {
    @JvmStatic fun getPathHashCode(file: VirtualFile): Int =
            file.path.toLowerCase().hashCode()

    private val PART_CLASS_NAME_SUFFIX = "Kt"

    @JvmStatic fun getPartClassName(str: String): String =
            if (str.isEmpty())
                "_$PART_CLASS_NAME_SUFFIX"
            else
                capitalizeAsJavaClassName(JvmAbi.sanitizeAsJavaIdentifier(str)) + PART_CLASS_NAME_SUFFIX

    private @JvmStatic fun capitalizeAsJavaClassName(str: String): String =
            // NB use Locale.ENGLISH so that build is locale-independent.
            // See Javadoc on java.lang.String.toUpperCase() for more details.
            if (Character.isJavaIdentifierStart(str[0]))
                str.substring(0, 1).toUpperCase(Locale.ENGLISH) + str.substring(1)
            else
                "_$str"

    @TestOnly
    @JvmStatic fun getDefaultPartFqName(facadeClassFqName: FqName, file: VirtualFile): FqName =
            getPackagePartFqName(facadeClassFqName.parent(), file.name)

    @JvmStatic fun getPackagePartFqName(packageFqName: FqName, fileName: String): FqName {
        val partClassName = getFilePartShortName(fileName)
        return packageFqName.child(Name.identifier(partClassName))
    }

    @JvmStatic fun getFilesWithCallables(files: Collection<KtFile>): List<KtFile> =
            files.filter { fileHasTopLevelCallables(it) }

    @JvmStatic fun fileHasTopLevelCallables(file: KtFile): Boolean =
            file.declarations.any {
                it is KtProperty ||
                it is KtNamedFunction ||
                it is KtScript
            }

    @JvmStatic fun getFilePartShortName(fileName: String): String =
            getPartClassName(FileUtil.getNameWithoutExtension(fileName))

}
