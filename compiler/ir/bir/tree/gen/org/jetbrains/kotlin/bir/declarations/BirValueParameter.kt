/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirValueParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.valueParameter]
 */
interface BirValueParameter : BirElement, BirDeclaration, BirValueDeclaration, BirValueParameterSymbol {
    var index: Int
    var varargElementType: BirType?
    var isCrossinline: Boolean
    var isNoinline: Boolean
    /**
     * If `true`, the value parameter does not participate in [IdSignature] computation.
     *
     * This is a workaround that is needed for better support of compiler plugins.
     * Suppose you have the following code and some IR plugin that adds a value parameter to functions
     * marked with the `@PluginMarker` annotation.
     * ```kotlin
     * @PluginMarker
     * fun foo(defined: Int) { /* ... */ }
     * ```
     *
     * Suppose that after applying the plugin the function is changed to:
     * ```kotlin
     * @PluginMarker
     * fun foo(defined: Int, $extra: String) { /* ... */ }
     * ```
     *
     * If a compiler plugin adds parameters to an [BirFunction],
     * the representations of the function in the frontend and in the backend may diverge, potentially causing signature mismatch and
     * linkage errors (see [KT-40980](https://youtrack.jetbrains.com/issue/KT-40980)).
     * We wouldn't want IR plugins to affect the frontend representation, since in an IDE you'd want to be able to see those
     * declarations in their original form (without the `$extra` parameter).
     *
     * To fix this problem, [isHidden] was introduced.
     *
     * TODO: consider dropping [isHidden] if it isn't used by any known plugin.
     */
    var isHidden: Boolean
    var defaultValue: BirExpressionBody?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        defaultValue?.accept(data, visitor)
    }

    companion object : BirElementClass(BirValueParameter::class.java, 97, true)
}
