/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirExternalPackageFragment
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class BirExternalPackageFragmentImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor?,
    signature: IdSignature?,
    packageFqName: FqName,
    override val containerSource: DeserializedContainerSource?,
) : BirExternalPackageFragment() {
    override val owner: BirExternalPackageFragmentImpl
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
            BirChildElementList(this, 1)

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

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        declarations.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.declarations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
