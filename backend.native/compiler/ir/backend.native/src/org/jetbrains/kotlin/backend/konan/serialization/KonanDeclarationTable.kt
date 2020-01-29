package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class KonanGlobalDeclarationTable(signatureSerializer: IdSignatureSerializer, builtIns: IrBuiltIns) : GlobalDeclarationTable(signatureSerializer, KonanManglerIr) {
    init {
        loadKnownBuiltins(builtIns)
    }
}