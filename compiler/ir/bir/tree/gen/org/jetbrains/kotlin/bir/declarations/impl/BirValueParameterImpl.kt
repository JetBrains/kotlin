/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirValueParameterImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ParameterDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var type: BirType,
    override val isAssignable: Boolean,
    override var index: Int,
    override var varargElementType: BirType?,
    override var isCrossinline: Boolean,
    override var isNoinline: Boolean,
    override var isHidden: Boolean,
    defaultValue: BirExpressionBody?,
) : BirValueParameter() {
    private var _defaultValue: BirExpressionBody? = defaultValue

    override var defaultValue: BirExpressionBody?
        get() = _defaultValue
        set(value) {
            if (_defaultValue != value) {
                replaceChild(_defaultValue, value)
                _defaultValue = value
            }
        }
    init {
        initChild(_defaultValue)
    }
}
