/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.syntheticBody]
 */
abstract class BirSyntheticBody(elementClass: BirElementClass<*>) : BirBody(elementClass), BirElement {
    abstract var kind: IrSyntheticBodyKind

    companion object : BirElementClass<BirSyntheticBody>(BirSyntheticBody::class.java, 57, true)
}
