/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirEnumEntry
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirEnumEntryImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    initializerExpression: BirExpressionBody?,
    correspondingClass: BirClass?,
) : BirEnumEntry() {
    private var _initializerExpression: BirExpressionBody? = initializerExpression

    override var initializerExpression: BirExpressionBody?
        get() = _initializerExpression
        set(value) {
            if (_initializerExpression != value) {
                replaceChild(_initializerExpression, value)
                _initializerExpression = value
            }
        }

    private var _correspondingClass: BirClass? = correspondingClass

    override var correspondingClass: BirClass?
        get() = _correspondingClass
        set(value) {
            if (_correspondingClass != value) {
                replaceChild(_correspondingClass, value)
                _correspondingClass = value
            }
        }
    init {
        initChild(_initializerExpression)
        initChild(_correspondingClass)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._initializerExpression === old -> this.initializerExpression = new as
                BirExpressionBody
            this._correspondingClass === old -> this.correspondingClass = new as BirClass
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
