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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.sure

abstract class VirtualFileFinder : KotlinClassFinder {
    abstract fun findVirtualFileWithHeader(classId: ClassId): VirtualFile?

    override fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
        val file = findVirtualFileWithHeader(classId) ?: return null
        return KotlinBinaryClassCache.getKotlinBinaryClass(file)
    }

    override fun findKotlinClass(javaClass: JavaClass): KotlinJvmBinaryClass? {
        var file = (javaClass as JavaClassImpl).psi.containingFile?.virtualFile ?: return null
        if (javaClass.outerClass != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.parent!!.findChild(classFileName(javaClass) + ".class").sure { "Virtual file not found for $javaClass" }
        }

        return KotlinBinaryClassCache.getKotlinBinaryClass(file)
    }

    private fun classFileName(jClass: JavaClass): String {
        val simpleName = jClass.name.asString()
        val outerClass = jClass.outerClass ?: return simpleName
        return classFileName(outerClass) + "$" + simpleName
    }

    companion object SERVICE {
        fun getInstance(project: Project): VirtualFileFinder =
                VirtualFileFinderFactory.getInstance(project).create(GlobalSearchScope.allScope(project))
    }
}
