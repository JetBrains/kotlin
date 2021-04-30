/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.fir.FirModuleVisibilityChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.containerSource
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import java.nio.file.Paths

class FirJvmModuleVisibilityChecker(private val session: FirSession) : FirModuleVisibilityChecker {
    override fun isInFriendModule(declaration: FirMemberDeclaration): Boolean {
        val moduleInfo = (session.moduleData as? FirModuleInfoBasedModuleData)?.moduleInfo as? FirJvmModuleInfo ?: return false
        val binaryClass = when (val source = declaration.containerSource) {
            is KotlinJvmBinarySourceElement -> source.binaryClass
            is JvmPackagePartSource -> source.knownJvmBinaryClass
            else -> null
        } as? VirtualFileKotlinClass ?: return false
        // For incremental compilation, the already compiled part of the module should be accessible.
        return moduleInfo.friendPaths.any { binaryClass.isIn(it) } || moduleInfo.outputDirectory?.let { binaryClass.isIn(it) } == true
    }

    private fun VirtualFileKotlinClass.isIn(jarOrDirectory: String): Boolean =
        when (file.fileSystem.protocol) {
            StandardFileSystems.FILE_PROTOCOL ->
                VfsUtilCore.virtualToIoFile(file).toPath().startsWith(jarOrDirectory)
            StandardFileSystems.JAR_PROTOCOL ->
                VfsUtilCore.getVirtualFileForJar(file)?.let(VfsUtilCore::virtualToIoFile)?.toPath() == Paths.get(jarOrDirectory)
            else -> false
        }
}
