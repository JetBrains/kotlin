/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.InputStream

class DirectoryBasedClassFinder(
    val packageDirectory: VirtualFile,
    val directoryPackageFqName: FqName
) : KotlinClassFinder {
    override fun findKotlinClassOrContent(javaClass: JavaClass, jvmMetadataVersion: JvmMetadataVersion): KotlinClassFinder.Result? =
        findKotlinClassOrContent(javaClass.classId!!, jvmMetadataVersion)

    override fun findKotlinClassOrContent(classId: ClassId, jvmMetadataVersion: JvmMetadataVersion): KotlinClassFinder.Result? {
        if (classId.packageFqName != directoryPackageFqName) {
            return null
        }
        val targetName = classId.relativeClassName.pathSegments().joinToString("$", postfix = ".class")
        val virtualFile = packageDirectory.findChild(targetName)
        if (virtualFile != null && isKotlinWithCompatibleAbiVersion(virtualFile, jvmMetadataVersion)) {
            return ClsKotlinBinaryClassCache.getInstance().getKotlinBinaryClass(virtualFile)?.let(::KotlinClass)
        }
        return null
    }

    // TODO
    override fun findMetadata(classId: ClassId): InputStream? = null

    // TODO
    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? = null

    // TODO
    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    // TODO: load built-ins from packageDirectory?
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null
}

/**
 * Checks if this file is a compiled Kotlin class file ABI-compatible with the current plugin
 */
private fun isKotlinWithCompatibleAbiVersion(file: VirtualFile, jvmMetadataVersion: JvmMetadataVersion): Boolean {
    val clsKotlinBinaryClassCache = ClsKotlinBinaryClassCache.getInstance()
    if (!clsKotlinBinaryClassCache.isKotlinJvmCompiledFile(file)) return false

    val kotlinClass = clsKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(file)
    return kotlinClass != null && kotlinClass.metadataVersion.isCompatible(jvmMetadataVersion)
}


