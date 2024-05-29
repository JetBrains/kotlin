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

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead()
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    override val symbol: BirEnumEntrySymbol
        get() = this

    private var _initializerExpression: BirExpressionBody? = initializerExpression
    override var initializerExpression: BirExpressionBody?
        get() {
            recordPropertyRead()
            return _initializerExpression
        }
        set(value) {
            if (_initializerExpression !== value) {
                childReplaced(_initializerExpression, value)
                _initializerExpression = value
                invalidate()
            }
        }

    private var _correspondingClass: BirClass? = correspondingClass
    override var correspondingClass: BirClass?
        get() {
            recordPropertyRead()
            return _correspondingClass
        }
        set(value) {
            if (_correspondingClass !== value) {
                childReplaced(_correspondingClass, value)
                _correspondingClass = value
                invalidate()
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
