/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAnonymousInitializer
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature

class BirAnonymousInitializerImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var isStatic: Boolean,
    body: BirBlockBody,
) : BirAnonymousInitializer() {
    private var _body: BirBlockBody = body

    override var body: BirBlockBody
        get() = _body
        set(value) {
            if (_body != value) {
                replaceChild(_body, value)
                _body = value
            }
        }
    init {
        initChild(_body)
    }
}
