/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.isFromInteropLibrary
import org.jetbrains.kotlin.resolve.descriptorUtil.module

private fun IrDeclaration.findTopLevelDeclaration(): IrDeclaration = when (val parent = this.parent) {
    is IrDeclaration -> parent.findTopLevelDeclaration()
    else -> this
}

private fun IrDeclaration.propertyIfAccessor(): IrDeclaration =
    (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: this

fun DeclarationDescriptor.isFromInteropLibrary(): Boolean =
    this.isFromFirDeserializedInteropLibrary() || this.module.isFromInteropLibrary()

private fun DeclarationDescriptor.isFromFirDeserializedInteropLibrary(): Boolean {
    val declaration = (this as? IrBasedDeclarationDescriptor<*>)?.owner ?: return false

    // We need to find top-level non-accessor declaration, because
    //  - fir2ir lazy IR creates non-AbstractFir2IrLazyDeclaration declarations sometimes, e.g. for enum entries;
    //  - K2 metadata deserializer doesn't set containerSource for property accessors.
    val topLevelDeclaration = declaration.findTopLevelDeclaration().propertyIfAccessor()

    val firDeclaration = (topLevelDeclaration as? AbstractFir2IrLazyDeclaration<*>)?.fir as? FirMemberDeclaration ?: return false
    val containerSource = when (firDeclaration) {
        is FirCallableDeclaration -> firDeclaration.containerSource
        is FirClassLikeDeclaration -> firDeclaration.sourceElement
    }

    return containerSource is KlibDeserializedContainerSource && containerSource.isFromNativeInteropLibrary
}

/**
 * This function should be equivalent to `IrDeclaration.isFromInteropLibrary` from `backend.native`, but in fact it is not for declarations
 * from Fir modules.
 *
 * This should be fixed in the future.
 */
@ObsoleteDescriptorBasedAPI
fun IrDeclaration.isFromInteropLibraryByDescriptor() = descriptor.isFromInteropLibrary()
