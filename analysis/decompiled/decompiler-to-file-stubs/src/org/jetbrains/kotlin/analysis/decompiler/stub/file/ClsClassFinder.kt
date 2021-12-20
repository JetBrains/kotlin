/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.findKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

object ClsClassFinder {
    fun findMultifileClassParts(file: VirtualFile, classId: ClassId, partNames: List<String>): List<KotlinJvmBinaryClass> {
        val packageFqName = classId.packageFqName
        val partsFinder = DirectoryBasedClassFinder(file.parent!!, packageFqName)

        return partNames.mapNotNull {
            partsFinder.findKotlinClass(ClassId(packageFqName, Name.identifier(it.substringAfterLast('/'))))
        }
    }

    /**
     * Checks if this file is a compiled "internal" Kotlin class, i.e. a Kotlin class (not necessarily ABI-compatible with the current plugin)
     * which should NOT be decompiled (and, as a result, shown under the library in the Project view, be searchable via Find class, etc.)
     */
    fun isKotlinInternalCompiledFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        // Don't crash on invalid files (EA-97751)
        if (!file.isValid || fileContent?.size == 0 || !file.exists()) {
            return false
        }

        val clsKotlinBinaryClassCache = ClsKotlinBinaryClassCache.getInstance()

        if (!clsKotlinBinaryClassCache.isKotlinJvmCompiledFile(file, fileContent)) {
            return false
        }

        val innerClass =
            try {
                if (fileContent == null) {
                    ClassFileViewProvider.isInnerClass(file)
                } else {
                    ClassFileViewProvider.isInnerClass(file, fileContent)
                }
            } catch (exception: Exception) {
                Logger
                    .getInstance("org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder.isKotlinInternalCompiledFile")
                    .debug(file.path, exception)

                return false
            }

        if (innerClass) {
            return true
        }

        val header = clsKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(file, fileContent) ?: return false
        if (header.classId.isLocal) return true

        return header.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS ||
                header.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART
    }
}