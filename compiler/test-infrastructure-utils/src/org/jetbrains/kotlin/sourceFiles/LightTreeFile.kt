/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sourceFiles

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping

data class LightTreeFile(
    val lightTree: FlyweightCapableTreeStructure<LighterASTNode>,
    val sourceFile: KtSourceFile,
    val linesMapping: KtSourceFileLinesMapping
)
