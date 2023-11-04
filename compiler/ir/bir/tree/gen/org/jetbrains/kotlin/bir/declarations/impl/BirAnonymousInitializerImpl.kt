/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.BirImplChildElementList
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
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
    override val descriptor: ClassDescriptor?,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    isStatic: Boolean,
    body: BirBlockBody,
) : BirAnonymousInitializer() {
    override val owner: BirAnonymousInitializerImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(5)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(5)
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() {
            recordPropertyRead(6)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(6)
            }
        }

    override val annotations: BirChildElementList<BirConstructorCall> =
            BirImplChildElementList(this, 1, false)

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead(3)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(3)
            }
        }

    private var _isStatic: Boolean = isStatic

    override var isStatic: Boolean
        get() {
            recordPropertyRead(4)
            return _isStatic
        }
        set(value) {
            if (_isStatic != value) {
                _isStatic = value
                invalidate(4)
            }
        }

    private var _body: BirBlockBody? = body

    override var body: BirBlockBody
        get() {
            recordPropertyRead(2)
            return _body ?: throwChildElementRemoved("body")
        }
        set(value) {
            if (_body != value) {
                childReplaced(_body, value)
                _body = value
                invalidate(2)
            }
        }
    init {
        initChild(_body)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _body?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int = when {
        this._body === old -> {
            this._body = new as BirBlockBody?
            2
        }
        else -> throwChildForReplacementNotFound(old)
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.annotations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
