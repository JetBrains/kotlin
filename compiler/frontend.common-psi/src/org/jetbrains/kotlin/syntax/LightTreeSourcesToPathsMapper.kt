/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtSourceElement

/**
 * The mapper for LightTree-based sources. A file is identified by the root node of its light tree.
 */
class LightTreeSourcesToPathsMapper : NonPsiSourcesToPathsMapper<LighterASTNode>() {
    override val KtSourceElement.fileKey: LighterASTNode
        get() = (this as KtLightSourceElement).treeStructure.root
}
