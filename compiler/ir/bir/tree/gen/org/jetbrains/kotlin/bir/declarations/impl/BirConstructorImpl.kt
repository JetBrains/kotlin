/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirConstructorImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var isExternal: Boolean,
    override var visibility: DescriptorVisibility,
    override var isInline: Boolean,
    override var isExpect: Boolean,
    override var returnType: BirType,
    dispatchReceiverParameter: BirValueParameter?,
    extensionReceiverParameter: BirValueParameter?,
    override var contextReceiverParametersCount: Int,
    body: BirBody?,
    override var isPrimary: Boolean,
) : BirConstructor() {
    override var typeParameters: BirChildElementList<BirTypeParameter> =
            BirChildElementList(this, 0)

    private var _dispatchReceiverParameter: BirValueParameter? = dispatchReceiverParameter

    override var dispatchReceiverParameter: BirValueParameter?
        get() = _dispatchReceiverParameter
        set(value) {
            if (_dispatchReceiverParameter != value) {
                replaceChild(_dispatchReceiverParameter, value)
                _dispatchReceiverParameter = value
            }
        }

    private var _extensionReceiverParameter: BirValueParameter? = extensionReceiverParameter

    override var extensionReceiverParameter: BirValueParameter?
        get() = _extensionReceiverParameter
        set(value) {
            if (_extensionReceiverParameter != value) {
                replaceChild(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
            }
        }

    override var valueParameters: BirChildElementList<BirValueParameter> =
            BirChildElementList(this, 1)

    private var _body: BirBody? = body

    override var body: BirBody?
        get() = _body
        set(value) {
            if (_body != value) {
                replaceChild(_body, value)
                _body = value
            }
        }
    init {
        initChild(_dispatchReceiverParameter)
        initChild(_extensionReceiverParameter)
        initChild(_body)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._dispatchReceiverParameter === old -> this.dispatchReceiverParameter = new as
                BirValueParameter
            this._extensionReceiverParameter === old -> this.extensionReceiverParameter = new as
                BirValueParameter
            this._body === old -> this.body = new as BirBody
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.typeParameters
        id == 1 -> this.valueParameters
        else -> throwChildrenListWithIdNotFound(id)
    }
}
