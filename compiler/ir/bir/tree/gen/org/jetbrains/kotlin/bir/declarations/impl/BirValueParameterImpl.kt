/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirValueParameterImpl(
    sourceSpan: CompressedSourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    index: Int,
    varargElementType: BirType?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isHidden: Boolean,
    isAssignable: Boolean,
    defaultValue: BirExpressionBody?,
) : BirImplElementBase(BirValueParameter), BirValueParameter {
    override val owner: BirValueParameterImpl
        get() = this

    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan = sourceSpan

    override var signature: IdSignature? = signature

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override var type: BirType = type

    override var index: Int = index

    override var varargElementType: BirType? = varargElementType

    override var isCrossinline: Boolean = isCrossinline

    override var isNoinline: Boolean = isNoinline

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
    override var isHidden: Boolean = isHidden

    override var isAssignable: Boolean = isAssignable

    private var _defaultValue: BirExpressionBody? = defaultValue
    override var defaultValue: BirExpressionBody?
        get() {
            return _defaultValue
        }
        set(value) {
            if (_defaultValue !== value) {
                childReplaced(_defaultValue, value)
                _defaultValue = value
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)

    init {
        initChild(_defaultValue)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _defaultValue?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._defaultValue === old -> {
                this._defaultValue = new as BirExpressionBody?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.annotations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
