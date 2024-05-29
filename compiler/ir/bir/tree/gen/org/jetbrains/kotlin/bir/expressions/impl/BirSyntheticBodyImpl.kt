/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.expressions.BirSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind

class BirSyntheticBodyImpl(
    sourceSpan: SourceSpan,
    kind: IrSyntheticBodyKind,
) : BirSyntheticBody() {
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

    private var _kind: IrSyntheticBodyKind = kind
    override var kind: IrSyntheticBodyKind
        get() {
            recordPropertyRead()
            return _kind
        }
        set(value) {
            if (_kind != value) {
                _kind = value
                invalidate()
            }
        }

}
