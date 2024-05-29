/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class BirTypeParameterImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    variance: Variance,
    index: Int,
    isReified: Boolean,
    superTypes: List<BirType>,
) : BirTypeParameter(), BirTypeParameterSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        variance: Variance,
        index: Int,
        isReified: Boolean,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        variance = variance,
        index = index,
        isReified = isReified,
        superTypes = emptyList(),
    )

    override val owner: BirTypeParameterImpl
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

    override val symbol: BirTypeParameterSymbol
        get() = this

    private var _variance: Variance = variance
    override var variance: Variance
        get() {
            recordPropertyRead()
            return _variance
        }
        set(value) {
            if (_variance != value) {
                _variance = value
                invalidate()
            }
        }

    private var _index: Int = index
    override var index: Int
        get() {
            recordPropertyRead()
            return _index
        }
        set(value) {
            if (_index != value) {
                _index = value
                invalidate()
            }
        }

    private var _isReified: Boolean = isReified
    override var isReified: Boolean
        get() {
            recordPropertyRead()
            return _isReified
        }
        set(value) {
            if (_isReified != value) {
                _isReified = value
                invalidate()
            }
        }

    private var _superTypes: List<BirType> = superTypes
    override var superTypes: List<BirType>
        get() {
            recordPropertyRead()
            return _superTypes
        }
        set(value) {
            if (_superTypes != value) {
                _superTypes = value
                invalidate()
            }
        }

}
