/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirTypeAlias
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirTypeAliasImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: TypeAliasDescriptor?,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: DescriptorVisibility,
    isActual: Boolean,
    expandedType: BirType,
) : BirTypeAlias() {
    override val owner: BirTypeAliasImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(8)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(8)
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() {
            recordPropertyRead(9)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(9)
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

    private var _name: Name = name

    override var name: Name
        get() {
            recordPropertyRead(4)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate(4)
            }
        }

    private var _visibility: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead(5)
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate(5)
            }
        }

    override val typeParameters: BirChildElementList<BirTypeParameter> =
            BirImplChildElementList(this, 2, false)

    private var _isActual: Boolean = isActual

    override var isActual: Boolean
        get() {
            recordPropertyRead(6)
            return _isActual
        }
        set(value) {
            if (_isActual != value) {
                _isActual = value
                invalidate(6)
            }
        }

    private var _expandedType: BirType = expandedType

    override var expandedType: BirType
        get() {
            recordPropertyRead(7)
            return _expandedType
        }
        set(value) {
            if (_expandedType != value) {
                _expandedType = value
                invalidate(7)
            }
        }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        typeParameters.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.annotations
        2 -> this.typeParameters
        else -> throwChildrenListWithIdNotFound(id)
    }
}
