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

class SourcesToPathsMapper : FirSessionComponent {

    private val sourcesToPath = mutableMapOf<LighterASTNode, String>()

    fun registerFileSource(sourceElement: KtSourceElement, path: String) {
        if (sourceElement !is KtPsiSourceElement) {
            sourcesToPath[sourceElement.treeStructure.root] = path
        }
    }

    fun getSourceFilePath(sourceElement: KtSourceElement): String? {
        val psi = sourceElement.psi
        return if (psi != null) psi.containingFile?.virtualFile?.path else sourcesToPath[sourceElement.treeStructure.root]
    }
}

val FirSession.sourcesToPathsMapper: SourcesToPathsMapper by FirSession.sessionComponentAccessor()
