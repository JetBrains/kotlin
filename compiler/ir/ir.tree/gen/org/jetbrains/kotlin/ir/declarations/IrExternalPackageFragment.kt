/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * This is a root parent element for external declarations (meaning those that come from
 * another compilation unit/module, not to be confused with [IrPossiblyExternalDeclaration.isExternal]). 
 *
 * Each declaration is contained either in some [IrFile], or in some [IrExternalPackageFragment].
 * Declarations coming from dependencies are located in [IrExternalPackageFragment].
 *
 * It can be used for obtaining a module descriptor, which contains the information about
 * the module from which the declaration came. It would be more correct to have a link to some
 * [IrModuleFragment] instead, which would make [IrModuleFragment] the only source of truth about modules,
 * but this is how things are now.
 *
 * Also, it can be used for checking whether some declaration is external (by checking whether its top
 * level parent is an [IrExternalPackageFragment]). But it is not possible
 * to get all declarations from a fragment. Also, being in the same or different
 * fragment doesn't mean anything. There can be more than one fragment for the same dependency.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.externalPackageFragment]
 */
abstract class IrExternalPackageFragment : IrPackageFragment() {
    abstract override val symbol: IrExternalPackageFragmentSymbol

    abstract val containerSource: DeserializedContainerSource?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitExternalPackageFragment(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        declarations.transformInPlace(transformer, data)
    }
}
