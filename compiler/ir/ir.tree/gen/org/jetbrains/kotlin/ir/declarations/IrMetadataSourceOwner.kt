/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElement

/**
 * An [IrElement] capable of holding something which backends can use to write
 * as the metadata for the declaration.
 *
 * Technically, it can even be ± an array of bytes, but right now it's usually the frontend
 * representation of the declaration,
 * so a descriptor in case of K1, and [org.jetbrains.kotlin.fir.FirElement] in case of K2,
 * and the backend invokes a metadata serializer on it to obtain metadata and write it, for example,
 * to `@kotlin.Metadata`
 * on JVM.
 *
 * In Kotlin/Native, [metadata] is used to store some LLVM-related stuff in an IR declaration,
 * but this is only for performance purposes (before it was done using simple maps).
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.metadataSourceOwner]
 */
interface IrMetadataSourceOwner : IrElement {
    /**
     * The arbitrary metadata associated with this IR node.
     *
     * @see IrMetadataSourceOwner
     */
    var metadata: MetadataSource?
}
