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
    isAssignable: Boolean,
    index: Int,
    varargElementType: BirType?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isHidden: Boolean,
    defaultValue: BirExpressionBody?,
) : BirImplElementBase(BirValueParameter), BirValueParameter {
    override val owner: BirValueParameterImpl
        get() = this

    private var _sourceSpan: CompressedSourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan
        get() {
            recordPropertyRead()
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature? = signature
    override var signature: IdSignature?
        get() {
            recordPropertyRead()
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead()
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead()
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    private var _type: BirType = type
    override var type: BirType
        get() {
            recordPropertyRead()
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate()
            }
        }

    private var _isAssignable: Boolean = isAssignable
    override var isAssignable: Boolean
        get() {
            recordPropertyRead()
            return _isAssignable
        }
        set(value) {
            if (_isAssignable != value) {
                _isAssignable = value
                invalidate()
            }
        }

    private var _index: Int = index
    override var index: Int
        get() {
            recordPropertyRead()
            return _index
        }
        set(value) {
            if (_index != value) {
                _index = value
                invalidate()
            }
        }

    private var _varargElementType: BirType? = varargElementType
    override var varargElementType: BirType?
        get() {
            recordPropertyRead()
            return _varargElementType
        }
        set(value) {
            if (_varargElementType != value) {
                _varargElementType = value
                invalidate()
            }
        }

    private var _isCrossinline: Boolean = isCrossinline
    override var isCrossinline: Boolean
        get() {
            recordPropertyRead()
            return _isCrossinline
        }
        set(value) {
            if (_isCrossinline != value) {
                _isCrossinline = value
                invalidate()
            }
        }

    private var _isNoinline: Boolean = isNoinline
    override var isNoinline: Boolean
        get() {
            recordPropertyRead()
            return _isNoinline
        }
        set(value) {
            if (_isNoinline != value) {
                _isNoinline = value
                invalidate()
            }
        }

    private var _isHidden: Boolean = isHidden
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
    override var isHidden: Boolean
        get() {
            recordPropertyRead()
            return _isHidden
        }
        set(value) {
            if (_isHidden != value) {
                _isHidden = value
                invalidate()
            }
        }

    private var _defaultValue: BirExpressionBody? = defaultValue
    override var defaultValue: BirExpressionBody?
        get() {
            recordPropertyRead()
            return _defaultValue
        }
        set(value) {
            if (_defaultValue !== value) {
                childReplaced(_defaultValue, value)
                _defaultValue = value
                invalidate()
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
