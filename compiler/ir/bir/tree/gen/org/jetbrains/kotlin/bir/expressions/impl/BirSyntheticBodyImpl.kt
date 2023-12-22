/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.expressions.BirSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind

class BirSyntheticBodyImpl(
    sourceSpan: SourceSpan,
    kind: IrSyntheticBodyKind,
) : BirSyntheticBody(BirSyntheticBody) {
    private var _sourceSpan: SourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(2)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(2)
            }
        }

    private var _kind: IrSyntheticBodyKind = kind
    override var kind: IrSyntheticBodyKind
        get() {
            recordPropertyRead(1)
            return _kind
        }
        set(value) {
            if (_kind != value) {
                _kind = value
                invalidate(1)
            }
        }

}
