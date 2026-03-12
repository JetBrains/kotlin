/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.IdSignature

/**
 * Computes [IdSignature]s for CInterop klib declarations using IR stubs,
 * then adds the [IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY] flag.
 *
 * The IR stubs are enriched by [MetadataToIrStubConverter] with real ObjC annotations
 * so the standard [KonanManglerIr] can perform ObjC-specific mangling directly.
 */
internal class CInteropIdSignatureComputer {
    private val delegate = PublicIdSignatureComputer(KonanManglerIr)

    fun computeSignature(declaration: IrDeclaration): IdSignature =
        delegate.computeSignature(declaration).withInteropFlag()
}

private fun IdSignature.withInteropFlag(): IdSignature = when (this) {
    is IdSignature.CommonSignature -> IdSignature.CommonSignature(
        packageFqName, declarationFqName, id,
        mask or IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(true),
        description,
    )
    is IdSignature.AccessorSignature -> IdSignature.AccessorSignature(
        propertySignature.withInteropFlag(),
        accessorSignature.withInteropFlag() as IdSignature.CommonSignature,
    )
    is IdSignature.CompositeSignature -> IdSignature.CompositeSignature(
        container.withInteropFlag(), inner,
    )
    else -> this
}

/**
 * Reconstructs a kotlinx-metadata class name (e.g., `org/foo/Bar.Nested`)
 * from an [IrDeclarationParent] chain.
 */
internal fun findClassName(parent: IrDeclarationParent): String? {
    if (parent !is IrClass) return null
    val segments = mutableListOf<String>()
    var current: IrDeclarationParent = parent
    while (current is IrClass) {
        segments.add(0, current.name.asString())
        current = current.parent
    }
    val packageFqn = when (current) {
        is IrPackageFragment -> current.packageFqName.asString().replace('.', '/')
        else -> ""
    }
    val classSimpleName = segments.joinToString(".")
    return if (packageFqn.isEmpty()) classSimpleName else "$packageFqn/$classSimpleName"
}
