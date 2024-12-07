/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.ir.generator.irTransformerType
import org.jetbrains.kotlin.ir.generator.model.Element

internal fun Element.getTransformExplicitType(): Element {
    return generateSequence(this) { it.parentInVisitor }
        .firstNotNullOfOrNull {
            when {
                it.transformByChildren -> it.transformerReturnType ?: it
                else -> it.transformerReturnType
            }
        } ?: this
}

internal fun ImportCollecting.deprecatedVisitorInterface(newAbstractClass: ClassRef<*>) =
    """
    ## DEPRECATED
    This interface is deprecated and will be removed soon.
    Please use the [${newAbstractClass.render()}] abstract class instead.
    """.trimIndent()
