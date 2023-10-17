/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAnonymousInitializer
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature

class BirAnonymousInitializerImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor,
    signature: IdSignature?,
    override var annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    isStatic: Boolean,
    body: BirBlockBody,
) : BirAnonymousInitializer() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() = _sourceSpan
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() = _signature
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() = _origin
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _isStatic: Boolean = isStatic

    override var isStatic: Boolean
        get() = _isStatic
        set(value) {
            if (_isStatic != value) {
                _isStatic = value
                invalidate()
            }
        }

    private var _body: BirBlockBody = body

    override var body: BirBlockBody
        get() = _body
        set(value) {
            if (_body != value) {
                replaceChild(_body, value)
                _body = value
                invalidate()
            }
        }
    init {
        initChild(_body)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._body === old -> this.body = new as BirBlockBody
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
