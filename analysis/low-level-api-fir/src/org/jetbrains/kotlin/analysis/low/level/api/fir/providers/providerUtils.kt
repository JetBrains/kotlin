/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.DeserializedContainerSourceWithJvmClassName
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.JvmStubDeserializedBuiltInsContainerSource
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.createJavaClassFinderWithRawScope
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.TopPackageNamesProvider
import java.util.*

internal fun <T: Any> Optional<T>.getOrNull(): T? = orElse(null)

fun FirCallableSymbol<*>.jvmClassNameIfDeserialized(): JvmClassName? {
    return when (val containerSource = fir.containerSource) {
        is JvmStubDeserializedBuiltInsContainerSource -> containerSource.facadeClassName
        is FacadeClassSource -> containerSource.facadeClassName ?: containerSource.className
        is DeserializedContainerSourceWithJvmClassName -> containerSource.className
        is KotlinJvmBinarySourceElement -> JvmClassName.byClassId(containerSource.binaryClass.classId)
        else -> null
    }
}

// TODO (marco): Ugly code. Everything is copied. Polish.

fun Project.createLLJavaClassFinder(scope: GlobalSearchScope): JavaClassFinder =
    createJavaClassFinderWithRawScope(FilterOutKotlinFilesScope(this, scope))

// Filters out not only Kotlin source files, but also Kotlin class files. Otherwise, `getPackage` of Java symbol providers is too broad as
// it returns results for Kotlin classes.
private class FilterOutKotlinFilesScope(project: Project, baseScope: GlobalSearchScope) :
    DelegatingGlobalSearchScope(project, baseScope),
    TopPackageNamesProvider {

    override val topPackageNames: Set<String>?
        get() = (myBaseScope as? TopPackageNamesProvider)?.topPackageNames

    override fun contains(file: VirtualFile): Boolean {
        // KTIJ-20095: optimization to avoid heavy file.fileType calculation
        val extension = file.extension
        val ktFile =
            when {
                file.isDirectory -> false
                extension == KotlinFileType.EXTENSION -> true
                extension == JavaFileType.DEFAULT_EXTENSION -> false
                extension == JavaClassFileType.INSTANCE.defaultExtension ->
                    ClsKotlinBinaryClassCache.getInstance().isKotlinJvmCompiledFile(file)
                else -> {
                    val fileTypeByFileName = FileTypeRegistry.getInstance().getFileTypeByFileName(file.name)
                    fileTypeByFileName == KotlinFileType.INSTANCE || fileTypeByFileName == UnknownFileType.INSTANCE &&
                            FileTypeRegistry.getInstance().isFileOfType(file, KotlinFileType.INSTANCE)
                }
            }
        return !ktFile && myBaseScope.contains(file)
    }

    override fun toString() = "JCFI: $myBaseScope"
}
