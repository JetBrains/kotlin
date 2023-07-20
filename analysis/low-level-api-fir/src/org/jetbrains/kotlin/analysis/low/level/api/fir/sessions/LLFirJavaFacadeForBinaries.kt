/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry
import java.nio.file.Path
import java.nio.file.Paths

internal class LLFirJavaFacadeForBinaries(
    session: FirSession,
    builtinTypes: BuiltinTypes,
    classFinder: JavaClassFinder,
    private val binaryDependenciesModuleDataProvider: ModuleDataProvider,
) : FirJavaFacade(session, builtinTypes, classFinder) {
    override fun getModuleDataForClass(javaClass: JavaClass): FirModuleData {
        requireIsInstance<VirtualFileBoundJavaClass>(javaClass)
        val path = getBinaryPath(javaClass)
        return binaryDependenciesModuleDataProvider.getModuleData(path)
            ?: error("No module data found for ${javaClass.classId} with path $path and virtual file ${javaClass.virtualFile?.path}")
    }

    private fun getBinaryPath(javaClass: VirtualFileBoundJavaClass): Path {
        val virtualFile = javaClass.virtualFile
            ?: errorWithAttachment("no virtual file") {
                withEntry("javaClass", javaClass) { it.toString() }
            }
        val path = virtualFile.path
        return when {
            JAR_DELIMITER in path ->
                Paths.get(path.substringBefore(JAR_SEPARATOR))
            JAR_SEPARATOR in path && "modules/" in path -> {
                // CoreJrtFileSystem.CoreJrtHandler#findFile, which uses Path#resolve, finds a virtual file path to the file itself,
                // e.g., "/path/to/jdk/home!/modules/java.base/java/lang/Object.class". (JDK home path + JAR separator + actual file path)
                // URLs loaded from JDK, though, point to module names in a JRT protocol format,
                // e.g., "jrt:///path/to/jdk/home!/java.base" (JRT protocol prefix + JDK home path + JAR separator + module name)
                // After splitting at the JAR separator, it is regarded as a root directory "/java.base".
                // To work with LibraryPathFilter, a hacky workaround here is to remove "modules/" from actual file path.
                // e.g. "/path/to/jdk/home!/java.base/java/lang/Object.class", which, from Path viewpoint, belongs to "/java.base",
                // after splitting at the JAR separator, in a similar way.
                // See [StandaloneProjectFactory#getAllBinaryRoots] for a similar hack.
                Paths.get(path.replace("modules/", ""))
            }
            else ->
                Paths.get(path)
        }
    }

    companion object {
        const val JAR_DELIMITER = ".jar$JAR_SEPARATOR"
    }
}