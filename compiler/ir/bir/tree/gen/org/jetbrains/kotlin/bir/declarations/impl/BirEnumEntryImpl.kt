/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirEnumEntry
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirEnumEntrySymbol
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirEnumEntryImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    initializerExpression: BirExpressionBody?,
    correspondingClass: BirClass?,
) : BirImplElementBase(), BirEnumEntry, BirEnumEntrySymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        initializerExpression = null,
        correspondingClass = null,
    )

    override val owner: BirEnumEntryImpl
        get() = this

    override val isBound: Boolean
        get() = true

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override val symbol: BirEnumEntrySymbol
        get() = this

    private var _initializerExpression: BirExpressionBody? = initializerExpression
    override var initializerExpression: BirExpressionBody?
        get() {
            return _initializerExpression
        }
        set(value) {
            if (_initializerExpression !== value) {
                childReplaced(_initializerExpression, value)
                _initializerExpression = value
            }
        }

    private var _correspondingClass: BirClass? = correspondingClass
    override var correspondingClass: BirClass?
        get() {
            return _correspondingClass
        }
        set(value) {
            if (_correspondingClass !== value) {
                childReplaced(_correspondingClass, value)
                _correspondingClass = value
            }
        }


    init {
        initChild(_initializerExpression)
        initChild(_correspondingClass)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _initializerExpression?.acceptLite(visitor)
        _correspondingClass?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._initializerExpression === old -> {
                this._initializerExpression = new as BirExpressionBody?
            }
            this._correspondingClass === old -> {
                this._correspondingClass = new as BirClass?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
