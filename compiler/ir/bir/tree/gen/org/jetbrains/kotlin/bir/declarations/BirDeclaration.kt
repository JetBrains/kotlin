/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.declaration]
 */
interface BirDeclaration : BirElement, BirStatement, BirSymbolOwner, BirAnnotationContainerElement {
    var origin: IrDeclarationOrigin

    companion object : BirElementClass(BirDeclaration::class.java, 75, false)
}
