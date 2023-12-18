/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.DescToIrIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.IdSignature

class KonanGlobalDeclarationTable(builtIns: IrBuiltIns) : GlobalDeclarationTable(KonanManglerIr) {
    init {
        loadKnownBuiltins(builtIns)
    }
}

class KonanDeclarationTable(
        globalDeclarationTable: GlobalDeclarationTable
) : DeclarationTable(globalDeclarationTable) {

    private val signatureIdComposer = DescToIrIdSignatureComputer(KonanIdSignaturer(KonanManglerDesc))

    // TODO: We should get rid of this extension point in favor of proper support in IR-based mangler.
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? =
            if (declaration.isFromInteropLibraryByDescriptor()) {
                signatureIdComposer.computeSignature(declaration)
            } else null
}