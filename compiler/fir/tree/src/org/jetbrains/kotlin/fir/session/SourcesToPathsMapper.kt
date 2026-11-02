/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File

class SourcesToPathsMapper : FirSessionComponent {
    private val sourcesToPath = mutableMapOf<LighterASTNode, String>()
    private val pathToOwningSourceSet = mutableMapOf<String, String>()

    fun registerFileSource(sourceElement: KtSourceElement, path: String, owningSourceRootPath: String?) {
        if (sourceElement !is KtPsiSourceElement) {
            sourcesToPath[sourceElement.treeStructure.root] = path
        }
        owningSourceRootPath?.let {
            registerOwningSourceRoot(path, it)
        }
    }

    fun registerOwningSourceRoot(path: String, owningSourceRoot: String) {
        pathToOwningSourceSet[path] = owningSourceRoot
    }

    fun getSourceFilePath(sourceElement: KtSourceElement): String? {
        val psi = sourceElement.psi
        return if (psi != null) psi.containingFile?.virtualFile?.path else sourcesToPath[sourceElement.treeStructure.root]
    }

    fun getRelativeSourceFilePath(sourceElement: KtSourceElement): String? {
        val path = getSourceFilePath(sourceElement) ?: return null
        val owningSourceRoot = pathToOwningSourceSet[path] ?: return path
        return File(path).descendantRelativeTo(File(owningSourceRoot).absoluteFile).path
    }
}

val FirSession.sourcesToPathsMapper: SourcesToPathsMapper by FirSession.sessionComponentAccessor()
