/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * Determine if the [IrDeclaration] is from a C-interop library.
 *
 * Note: This implementation accesses FIR only when `this` is a lazy Fir2Ir declaration, and accesses descriptors
 * only when `this` is a LazyIr declaration. Thus, it becomes possible to use this function with deserialized
 * pure IR where FIR and descriptors might be unavailable.
 */
fun IrDeclaration.isFromCInteropLibrary(): Boolean {
    // We need to find top-level non-accessor declaration, because
    //  - fir2ir lazy IR creates non-AbstractFir2IrLazyDeclaration declarations sometimes, e.g. for enum entries;
    //  - K2 metadata deserializer doesn't set containerSource for property accessors.
    val topLevelDeclaration = findTopLevelDeclaration().propertyIfAccessor()

    val containerSource = if (topLevelDeclaration is AbstractFir2IrLazyDeclaration<*>)
        getSourceElementFromFir(topLevelDeclaration)
    else
        getSourceElementFromDescriptor(topLevelDeclaration)

    return containerSource is KlibDeserializedContainerSource && containerSource.isFromCInteropLibrary
}

/**
 * Determine if the [DeclarationDescriptor] is from a C-interop library.
 */
fun DeclarationDescriptor.isFromCInteropLibrary(): Boolean {
    return when (this) {
        is ModuleDescriptor -> isCInteropLibraryModule()
        is IrBasedDeclarationDescriptor<*> -> owner.isFromCInteropLibrary()
        else -> module.isCInteropLibraryModule()
    }
}

private fun getSourceElementFromFir(topLevelLazyFir2IrDeclaration: AbstractFir2IrLazyDeclaration<*>): SourceElement? =
    when (val firDeclaration = topLevelLazyFir2IrDeclaration.fir as? FirMemberDeclaration) {
        is FirCallableDeclaration -> firDeclaration.containerSource
        is FirClassLikeDeclaration -> firDeclaration.sourceElement
        else -> null
    }

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun getSourceElementFromDescriptor(topLevelDeclaration: IrDeclaration): SourceElement? {
    val topLevelDeclarationDescriptor = if (topLevelDeclaration is IrLazyDeclarationBase) {
        // There is always some descriptor.
        topLevelDeclaration.descriptor
    } else {
        // There can be no descriptor. So take if with caution.
        val symbol = topLevelDeclaration.symbol
        if (symbol.hasDescriptor) symbol.descriptor else null
    }

    return (topLevelDeclarationDescriptor?.containingDeclaration as? PackageFragmentDescriptor)?.source
}

private fun IrDeclaration.findTopLevelDeclaration(): IrDeclaration = when (val parent = this.parent) {
    is IrDeclaration -> parent.findTopLevelDeclaration()
    else -> this
}

private fun IrDeclaration.propertyIfAccessor(): IrDeclaration =
    (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: this

private fun ModuleDescriptor.isCInteropLibraryModule(): Boolean {
    return if (this is ModuleDescriptorImpl) {
        // cinterop libraries are deserialized by Fir2Ir as ModuleDescriptorImpl, not FirModuleDescriptor
        val moduleOrigin = klibModuleOrigin
        moduleOrigin is DeserializedKlibModuleOrigin && moduleOrigin.library.isCInteropLibrary()
    } else false
}

@Deprecated(
    "Use isFromCInteropLibrary() instead",
    ReplaceWith("isFromCInteropLibrary()", "org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary"),
    DeprecationLevel.ERROR
)
fun IrDeclaration.isFromInteropLibrary(): Boolean = isFromCInteropLibrary()

@Deprecated(
    "Use isFromCInteropLibrary() instead",
    ReplaceWith("isFromCInteropLibrary()", "org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary"),
    DeprecationLevel.ERROR
)
fun DeclarationDescriptor.isFromInteropLibrary(): Boolean = isFromCInteropLibrary()
