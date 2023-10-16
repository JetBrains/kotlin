/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirLocalDelegatedProperty
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirLocalDelegatedPropertyImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptorWithAccessors,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var metadata: MetadataSource?,
    override var type: BirType,
    override var isVar: Boolean,
    delegate: BirVariable,
    getter: BirSimpleFunction,
    setter: BirSimpleFunction?,
) : BirLocalDelegatedProperty() {
    private var _delegate: BirVariable = delegate

    override var delegate: BirVariable
        get() = _delegate
        set(value) {
            if (_delegate != value) {
                replaceChild(_delegate, value)
                _delegate = value
            }
        }

    private var _getter: BirSimpleFunction = getter

    override var getter: BirSimpleFunction
        get() = _getter
        set(value) {
            if (_getter != value) {
                replaceChild(_getter, value)
                _getter = value
            }
        }

    private var _setter: BirSimpleFunction? = setter

    override var setter: BirSimpleFunction?
        get() = _setter
        set(value) {
            if (_setter != value) {
                replaceChild(_setter, value)
                _setter = value
            }
        }
    init {
        initChild(_delegate)
        initChild(_getter)
        initChild(_setter)
    }
}
