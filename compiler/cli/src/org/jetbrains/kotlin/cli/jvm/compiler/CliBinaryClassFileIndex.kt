/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileIndex
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileScope
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.asBinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.virtualFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * [BinaryClassFileIndex] over the CLI [JvmDependenciesIndex].
 */
class CliBinaryClassFileIndex(
    private val index: JvmDependenciesIndex,
    enableSearchInCtSym: Boolean,
) : BinaryClassFileIndex {

    private val extensions: JavaFileExtensions =
        if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS

    override fun findTopLevelClassFiles(topLevelClassId: ClassId): Collection<BinaryClassFileHandle> =
        index.findClassVirtualFiles(topLevelClassId, extensions).map { it.asBinaryClassFileHandle() }

    override fun classFileNamesInPackage(packageFqName: FqName): Set<String> {
        val result = LinkedHashSet<String>()
        index.traverseClassVirtualFilesInPackage(packageFqName, extensions) { file ->
            result.add(file.nameWithoutExtension)
            true
        }
        return result
    }

    override fun containsPackageDirectory(packageFqName: FqName): Boolean {
        var found = false
        index.traverseDirectoriesInPackage(packageFqName, JavaRoot.OnlyBinary) { _, _ ->
            found = true
            false // stop at the first hit
        }
        return found
    }

    private companion object {
        private val BINARY_CLASS_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS)
        private val BINARY_CLASS_AND_SIG_EXTENSIONS =
            JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
    }
}

fun CliVirtualFileFinderFactory.binaryClassFileIndex(): BinaryClassFileIndex =
    CliBinaryClassFileIndex(index, enableSearchInCtSym)

fun VfsBasedProjectEnvironment.binaryClassFileScope(classpath: JvmClasspath): BinaryClassFileScope {
    val psiScope = psiSearchScope(classpath)
    return BinaryClassFileScope { classFile -> classFile.virtualFile in psiScope }
}
