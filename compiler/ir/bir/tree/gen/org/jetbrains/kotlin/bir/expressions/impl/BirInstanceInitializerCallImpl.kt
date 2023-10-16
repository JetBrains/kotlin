/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirInstanceInitializerCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirType

class BirInstanceInitializerCallImpl(
    override var sourceSpan: SourceSpan,
    override var type: BirType,
    override var classSymbol: BirClassSymbol,
) : BirInstanceInitializerCall() {
    override var attributeOwnerId: BirAttributeContainer = this

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
