/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirExternalPackageFragment
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class BirExternalPackageFragmentImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    override val declarations: MutableList<BirDeclaration>,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor,
    override var signature: IdSignature,
    override var packageFqName: FqName,
    override val containerSource: DeserializedContainerSource?,
) : BirExternalPackageFragment()
