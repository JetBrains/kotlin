/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.util

import com.intellij.java.syntax.JavaSyntaxDefinition
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree

internal fun LightSyntaxTree.childrenWithoutCommentsAndWhitespace(node: LightNode): List<LightNode> =
    getChildren(node).filterNot { child ->
        val type = getType(child)
        type == SyntaxTokenTypes.WHITE_SPACE || type in JavaSyntaxDefinition.comments
    }