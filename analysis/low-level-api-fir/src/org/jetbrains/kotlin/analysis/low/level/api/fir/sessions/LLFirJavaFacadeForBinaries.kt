/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

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
            ?: error("No module data found for ${javaClass.classId} with path $path")
    }

    private fun getBinaryPath(javaClass: VirtualFileBoundJavaClass): Path {
        val virtualFile = javaClass.virtualFile
            ?: error("no virtual file for ${javaClass.classId}")
        val path = virtualFile.path
        return Paths.get(path)
    }
}