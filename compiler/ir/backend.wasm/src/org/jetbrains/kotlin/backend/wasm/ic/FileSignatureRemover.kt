/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName

internal fun fileSignatureErasure(idSignature: IdSignature, moduleName: String): IdSignature =
    if (idSignature.containsFileSignature(moduleName)) idSignature.rebuildSignature(moduleName) else idSignature

private fun IdSignature.containsFileSignature(moduleName: String): Boolean = when (this) {
    is IdSignature.AccessorSignature -> propertySignature.containsFileSignature(moduleName)
    is IdSignature.CommonSignature -> false
    is IdSignature.CompositeSignature -> container.containsFileSignature(moduleName) || inner.containsFileSignature(moduleName)
    is IdSignature.FileLocalSignature -> container.containsFileSignature(moduleName)
    is IdSignature.LocalSignature -> false
    is IdSignature.LoweredDeclarationSignature -> original.containsFileSignature(moduleName)
    is IdSignature.ScopeLocalDeclaration -> false
    is IdSignature.SpecialFakeOverrideSignature ->
        memberSignature.containsFileSignature(moduleName) || overriddenSignatures.any { it.containsFileSignature(moduleName) }
    is IdSignature.FileSignature -> true
}

private fun IdSignature.rebuildSignature(moduleName: String) = when (this) {
    is IdSignature.AccessorSignature -> rebuildSignature(moduleName)
    is IdSignature.CommonSignature -> this
    is IdSignature.CompositeSignature -> rebuildSignature(moduleName)
    is IdSignature.FileLocalSignature -> rebuildSignature(moduleName)
    is IdSignature.LocalSignature -> this
    is IdSignature.LoweredDeclarationSignature -> rebuildSignature(moduleName)
    is IdSignature.ScopeLocalDeclaration -> this
    is IdSignature.SpecialFakeOverrideSignature -> rebuildSignature(moduleName)
    is IdSignature.FileSignature -> rebuildSignature(moduleName)
}

private fun IdSignature.AccessorSignature.rebuildSignature(moduleName: String): IdSignature =
    IdSignature.AccessorSignature(propertySignature.rebuildSignature(moduleName), accessorSignature)

private fun IdSignature.CompositeSignature.rebuildSignature(moduleName: String): IdSignature =
    IdSignature.CompositeSignature(container.rebuildSignature(moduleName), inner.rebuildSignature(moduleName))

private fun IdSignature.FileLocalSignature.rebuildSignature(moduleName: String): IdSignature =
    IdSignature.FileLocalSignature(container.rebuildSignature(moduleName), id, description)

private fun IdSignature.LoweredDeclarationSignature.rebuildSignature(moduleName: String): IdSignature =
    IdSignature.LoweredDeclarationSignature(original.rebuildSignature(moduleName), stage, index)


private fun IdSignature.SpecialFakeOverrideSignature.rebuildSignature(moduleName: String): IdSignature =
    IdSignature.SpecialFakeOverrideSignature(
        memberSignature.rebuildSignature(moduleName),
        overriddenSignatures.map { it.rebuildSignature(moduleName) }
    )

private fun IdSignature.FileSignature.rebuildSignature(moduleName: String): IdSignature =
    (moduleName + fileName).let { IdSignature.FileSignature(it, FqName.ROOT, it) }
