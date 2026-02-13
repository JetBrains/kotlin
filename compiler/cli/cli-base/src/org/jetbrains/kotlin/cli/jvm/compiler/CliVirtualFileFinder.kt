/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.google.common.cache.CacheBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DOT_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.InputStream
import java.util.concurrent.TimeUnit

class CliVirtualFileFinder(
    private val index: JvmDependenciesIndex,
    private val scope: GlobalSearchScope,
    private val enableSearchInCtSym: Boolean,
    perfManager: PerformanceManager?,
) : VirtualFileFinder(perfManager) {
    private val childrenInPackageCache =
        CacheBuilder.newBuilder()
            .maximumSize(2000)
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build<FqName, Map<String, Set<VirtualFile>>>()

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
        findBinaryOrSigClass(classId)

    override fun findSourceOrBinaryVirtualFile(classId: ClassId) =
        findBinaryOrSigClass(classId)
            ?: findClass(classId, classId.relativeClassName.asString() + ".java")

    override fun findMetadata(classId: ClassId): InputStream? {
        assert(!classId.isNestedClass) { "Nested classes are not supported here: $classId" }

        return findClass(
            classId,
            classId.shortClassName.asString() + DOT_METADATA_FILE_EXTENSION
        )?.inputStream
    }

    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String> {
        val result = ObjectOpenHashSet<String>()
        childrenInPackage(packageFqName).values
            .flatMap { it }
            .filter { it.extension == METADATA_FILE_EXTENSION }
            .forEach { result.add(it.nameWithoutExtension) }
        return result
    }

    override fun hasMetadataPackage(fqName: FqName): Boolean {
        var found = false
        index.traverseDirectoriesInPackage(fqName, continueSearch = { dir, _ ->
            found = found or dir.children.any { it.extension == METADATA_FILE_EXTENSION }
            !found
        })
        return found
    }

    override fun findBuiltInsData(packageFqName: FqName): InputStream? {
        // "<builtins-metadata>" is just a made-up name
        // JvmDependenciesIndex requires the ClassId of the class which we're searching for, to cache the last request+result
        val classId = ClassId(packageFqName, Name.special("<builtins-metadata>"))

        return findClass(classId, BuiltInSerializerProtocol.getBuiltInsFileName(packageFqName))?.inputStream
    }

    private fun findClass(classId: ClassId, fileName: String) =
        childrenInPackage(classId.packageFqName)[fileName]
            ?.filter(VirtualFile::isValid)
            ?.firstOrNull { it in scope }

    private fun findBinaryOrSigClass(classId: ClassId): VirtualFile? {
        val simpleName = classId.relativeClassName.asString().replace('.', '$')
        val cache = childrenInPackage(classId.packageFqName)
        cache["$simpleName.class"]?.firstOrNull { it in scope }?.let {
            return it
        }
        return if (enableSearchInCtSym) {
            cache["$simpleName.sig"]?.firstOrNull { it in scope }
        } else {
            null
        }
    }

    private fun childrenInPackage(packageFqName: FqName): Map<String, Set<VirtualFile>> {
        return childrenInPackageCache.get(packageFqName) {
            val files = Object2ObjectOpenHashMap<String, MutableSet<VirtualFile>>()
            index.traverseDirectoriesInPackage(packageFqName, continueSearch = { dir, _ ->
                for (child in dir.children) {
                    if (child.extension == METADATA_FILE_EXTENSION ||
                        child.extension == "class" ||
                        child.extension == "sig" ||
                        child.extension == "java"
                    ) {
                        files.getOrPut(child.name) { ObjectOpenHashSet() }.add(child)
                    }
                }

                true
            })
            files
        }
    }
}
