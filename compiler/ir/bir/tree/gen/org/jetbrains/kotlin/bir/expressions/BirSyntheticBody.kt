/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind

abstract class BirSyntheticBody() : BirBody() {
    abstract var kind: IrSyntheticBodyKind

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirSyntheticBody

    companion object : BirElementClass<BirSyntheticBody>(BirSyntheticBody::class.java, 91, true)
}
