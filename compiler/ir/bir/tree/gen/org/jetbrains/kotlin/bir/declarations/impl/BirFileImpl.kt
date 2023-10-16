/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName

class BirFileImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor,
    override var signature: IdSignature,
    override var packageFqName: FqName,
    override var annotations: List<BirConstructorCall>,
    override var module: BirModuleFragment,
    override var fileEntry: IrFileEntry,
) : BirFile() {
    override val declarations: BirChildElementList<BirDeclaration> =
            BirChildElementList(this, 0)

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.declarations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
