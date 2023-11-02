/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName

class BirFileImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor?,
    signature: IdSignature?,
    packageFqName: FqName,
    fileEntry: IrFileEntry,
) : BirFile() {
    override val owner: BirFileImpl
        get() = this

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

    override val declarations: BirChildElementList<BirDeclaration> =
            BirImplChildElementList(this, 1, false)

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() {
            recordPropertyRead()
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    private var _packageFqName: FqName = packageFqName

    override var packageFqName: FqName
        get() {
            recordPropertyRead()
            return _packageFqName
        }
        set(value) {
            if (_packageFqName != value) {
                _packageFqName = value
                invalidate()
            }
        }

    override val annotations: BirChildElementList<BirConstructorCall> =
            BirImplChildElementList(this, 2, false)

    private var _fileEntry: IrFileEntry = fileEntry

    override var fileEntry: IrFileEntry
        get() {
            recordPropertyRead()
            return _fileEntry
        }
        set(value) {
            if (_fileEntry != value) {
                _fileEntry = value
                invalidate()
            }
        }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        declarations.acceptChildrenLite(visitor)
        annotations.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.declarations
        2 -> this.annotations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
