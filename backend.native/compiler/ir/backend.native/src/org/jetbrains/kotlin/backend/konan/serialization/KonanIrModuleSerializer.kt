package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol

class KonanIrModuleSerializer(
    logger: LoggingContext,
    irBuiltIns: IrBuiltIns,
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    val skipExpects: Boolean
) : IrModuleSerializer<KonanIrFileSerializer>(logger) {

    private val signaturer = IdSignatureSerializer(KonanManglerIr)
    private val globalDeclarationTable = KonanGlobalDeclarationTable(signaturer, irBuiltIns)

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer =
            KonanIrFileSerializer(logger, DeclarationTable(globalDeclarationTable), expectDescriptorToSymbol, skipExpects = skipExpects)
}
