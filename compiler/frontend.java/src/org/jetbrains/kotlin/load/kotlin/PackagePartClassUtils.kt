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
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.*

public object PackagePartClassUtils {
    @JvmStatic
    public fun getPathHashCode(file: VirtualFile): Int =
            file.path.toLowerCase().hashCode()

    private val PART_CLASS_NAME_SUFFIX = "Kt"

    public @JvmStatic fun getPartClassName(str: String): String =
            if (str.isEmpty())
                "_$PART_CLASS_NAME_SUFFIX"
            else
                capitalizeAsJavaClassName(JvmAbi.sanitizeAsJavaIdentifier(str)) + PART_CLASS_NAME_SUFFIX

    private @JvmStatic fun capitalizeAsJavaClassName(str: String): String =
            // NB use Locale.ENGLISH so that build is locale-independent.
            // See Javadoc on java.lang.String.toUpperCase() for more details.
            if (Character.isJavaIdentifierStart(str.charAt(0)))
                str.substring(0, 1).toUpperCase(Locale.ENGLISH) + str.substring(1)
            else
                "_$str"

    @TestOnly
    @JvmStatic
    public fun getDefaultFileClassFqName(packageFqName: FqName, file: VirtualFile): FqName =
            getPackagePartFqName(packageFqName, file.name)

    @TestOnly
    @JvmStatic
    public fun getDefaultPartFqName(facadeClassFqName: FqName, file: VirtualFile): FqName =
            getPackagePartFqName(facadeClassFqName.parent(), file.name)

    @JvmStatic
    public fun getPackagePartFqName(packageFqName: FqName, fileName: String): FqName {
        val partClassName = getFilePartShortName(fileName)
        return packageFqName.child(Name.identifier(partClassName))
    }

    @Deprecated("Migrate to JvmFileClassesProvider")
    @JvmStatic
    public fun getPackagePartInternalName(file: KtFile): String =
            JvmClassName.byFqNameWithoutInnerClasses(getPackagePartFqName(file)).internalName

    @Deprecated("Migrate to JvmFileClassesProvider")
    @JvmStatic
    public fun getPackagePartFqName(file: KtFile): FqName =
            getPackagePartFqName(file.packageFqName, file.name)

    @JvmStatic
    public fun getFilesWithCallables(files: Collection<KtFile>): List<KtFile> =
            files.filter { fileHasTopLevelCallables(it) }

    @JvmStatic
    public fun fileHasTopLevelCallables(file: KtFile): Boolean =
            file.declarations.any { it is KtProperty || it is KtNamedFunction }

    @JvmStatic
    public fun getFilePartShortName(fileName: String): String =
            getPartClassName(FileUtil.getNameWithoutExtension(fileName))

}