/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementBackReferencesKey
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

abstract class BirGetValue() : BirValueAccessExpression() {
    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirGetValue

    companion object : BirElementClass<BirGetValue>(BirGetValue::class.java, 58, true) {
        val symbol = BirElementBackReferencesKey<BirGetValue, _>{ (it as? BirGetValue)?.symbol?.owner }
    }
}
