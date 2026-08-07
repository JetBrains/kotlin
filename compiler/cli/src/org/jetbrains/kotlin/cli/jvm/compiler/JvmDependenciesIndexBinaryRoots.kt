/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassRoots
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.TopLevelClassFileCandidates
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.asBinaryClassFileHandle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * [BinaryClassRoots] over the CLI [JvmDependenciesIndex]: the only place where the search scope of a
 * binary session is a PSI [GlobalSearchScope], so that consumers of the seam need neither PSI nor VFS.
 */
class JvmDependenciesIndexBinaryRoots(
    private val index: JvmDependenciesIndex,
    private val scope: GlobalSearchScope,
    enableSearchInCtSym: Boolean,
) : BinaryClassRoots {

    private val extensions: JavaFileExtensions =
        if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS

    override fun findTopLevelClassFiles(topLevelClassId: ClassId): TopLevelClassFileCandidates {
        var anywhere: VirtualFile? = null
        var inScope: VirtualFile? = null
        for (candidate in index.findClassVirtualFiles(topLevelClassId, extensions)) {
            if (anywhere == null) anywhere = candidate
            if (candidate in scope) {
                inScope = candidate
                break
            }
        }
        return TopLevelClassFileCandidates(anywhere?.asBinaryClassFileHandle(), inScope?.asBinaryClassFileHandle())
    }

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

/**
 * Views this factory's classpath index through the scope of a single session.
 *
 * The `ct.sym` `.sig` extension choice comes from the factory, so a `-Xjdk-release` build keeps
 * resolving JDK API stubs.
 */
@Suppress("UnstableApiUsage")
fun CliVirtualFileFinderFactory.binaryClassRootsForScope(): (AbstractProjectFileSearchScope) -> BinaryClassRoots =
    { scope -> JvmDependenciesIndexBinaryRoots(index, scope.asPsiSearchScope(), enableSearchInCtSym) }
