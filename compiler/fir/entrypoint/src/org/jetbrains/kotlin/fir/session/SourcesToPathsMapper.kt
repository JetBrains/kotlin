/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.psi

class SourcesToPathsMapper : FirSessionComponent {

    private val sourcesToPath = mutableMapOf<LighterASTNode, String>()
    private val sourcesToLinesMapping = mutableMapOf<LighterASTNode, KtSourceFileLinesMapping>()

    fun registerFileSource(sourceElement: KtSourceElement, path: String, linesMapping: KtSourceFileLinesMapping? = null) {
        if (sourceElement !is KtPsiSourceElement) {
            val root = sourceElement.treeStructure.root
            sourcesToPath[root] = path
            if (linesMapping != null) {
                sourcesToLinesMapping[root] = linesMapping
            }
        }
    }

    fun getSourceFilePath(sourceElement: KtSourceElement): String? {
        val psi = sourceElement.psi
        return if (psi != null) psi.containingFile?.virtualFile?.path else sourcesToPath[sourceElement.treeStructure.root]
    }

    /**
     * Returns the lines mapping of the light tree file containing [sourceElement], or `null` for PSI-based sources
     * and files registered without a mapping.
     */
    fun getSourceFileLinesMapping(sourceElement: KtSourceElement): KtSourceFileLinesMapping? {
        if (sourceElement is KtPsiSourceElement) return null
        return sourcesToLinesMapping[sourceElement.treeStructure.root]
    }
}

val FirSession.sourcesToPathsMapper: SourcesToPathsMapper by FirSession.sessionComponentAccessor()
