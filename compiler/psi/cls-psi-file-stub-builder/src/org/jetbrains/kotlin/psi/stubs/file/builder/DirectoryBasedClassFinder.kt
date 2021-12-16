/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.file.builder

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.InputStream

class DirectoryBasedClassFinder(
    val packageDirectory: VirtualFile,
    val directoryPackageFqName: FqName
) : KotlinClassFinder {
    override fun findKotlinClassOrContent(javaClass: JavaClass): KotlinClassFinder.Result? = findKotlinClassOrContent(javaClass.classId!!)

    override fun findKotlinClassOrContent(classId: ClassId): KotlinClassFinder.Result? {
        if (classId.packageFqName != directoryPackageFqName) {
            return null
        }
        val targetName = classId.relativeClassName.pathSegments().joinToString("$", postfix = ".class")
        val virtualFile = packageDirectory.findChild(targetName)
        if (virtualFile != null && isKotlinWithCompatibleAbiVersion(virtualFile)) {
            return IDEKotlinBinaryClassCache.getInstance().getKotlinBinaryClass(virtualFile)?.let(::KotlinClass)
        }
        return null
    }

    // TODO
    override fun findMetadata(classId: ClassId): InputStream? = null

    // TODO
    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    // TODO: load built-ins from packageDirectory?
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null
}

/**
 * Checks if this file is a compiled Kotlin class file ABI-compatible with the current plugin
 */
private fun isKotlinWithCompatibleAbiVersion(file: VirtualFile): Boolean {
    val ideKotlinBinaryClassCache = IDEKotlinBinaryClassCache.getInstance()
    if (!ideKotlinBinaryClassCache.isKotlinJvmCompiledFile(file)) return false

    val kotlinClass = ideKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(file)
    return kotlinClass != null && kotlinClass.metadataVersion.isCompatible()
}


