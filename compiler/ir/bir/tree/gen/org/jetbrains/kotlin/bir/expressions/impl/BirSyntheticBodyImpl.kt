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
    override var sourceSpan: SourceSpan = sourceSpan

    override var kind: IrSyntheticBodyKind = kind

}
