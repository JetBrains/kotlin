/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter

internal fun computeTypeParameters(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): List<JavaTypeParameter> {
    val typeParamNodes = tree.findChildByType(node, JavaSyntaxElementType.TYPE_PARAMETER_LIST)
        ?.let { tree.getChildrenByType(it, JavaSyntaxElementType.TYPE_PARAMETER) }
        ?: return emptyList()

    // Create type parameter instances first
    val typeParams = typeParamNodes.map { JavaTypeParameterOverAst(it, tree, resolutionContext) }

    // Create a resolution context with ALL type parameters in scope.
    // This is needed for resolving bounds like `<E, S extends List<E>>`.
    val contextWithTypeParams = resolutionContext.withTypeParameters(typeParams)

    // Update each type parameter to use the enriched context for bounds resolution
    typeParams.forEach { it.updateResolutionContext(contextWithTypeParams) }

    return typeParams
}
