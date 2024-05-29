/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAnonymousInitializer
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirAnonymousInitializerSymbol
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature

class BirAnonymousInitializerImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    isStatic: Boolean,
    body: BirBlockBody,
) : BirAnonymousInitializer(), BirAnonymousInitializerSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        isStatic: Boolean,
        body: BirBlockBody,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        isStatic = isStatic,
        body = body,
    )

    override val owner: BirAnonymousInitializerImpl
        get() = this

    override val isBound: Boolean
        get() = true

    private var _sourceSpan: SourceSpan = sourceSpan
    override var sourceSpan: SourceSpan
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

    override val signature: IdSignature? = signature

    private var _annotations: List<BirConstructorCall> = annotations
    override var annotations: List<BirConstructorCall>
        get() {
            recordPropertyRead()
            return _annotations
        }
        set(value) {
            if (_annotations != value) {
                _annotations = value
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

    override val symbol: BirAnonymousInitializerSymbol
        get() = this

    private var _isStatic: Boolean = isStatic
    override var isStatic: Boolean
        get() {
            recordPropertyRead()
            return _isStatic
        }
        set(value) {
            if (_isStatic != value) {
                _isStatic = value
                invalidate()
            }
        }

    private var _body: BirBlockBody? = body
    override var body: BirBlockBody
        get() {
            recordPropertyRead()
            return _body ?: throwChildElementRemoved("body")
        }
        set(value) {
            if (_body !== value) {
                childReplaced(_body, value)
                _body = value
                invalidate()
            }
        }


    init {
        initChild(_body)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _body?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._body === old -> {
                this._body = new as BirBlockBody?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
