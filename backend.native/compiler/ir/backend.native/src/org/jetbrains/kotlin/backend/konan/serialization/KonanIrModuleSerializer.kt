package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module

private class KonanDeclarationTable(
        descriptorTable: DescriptorTable,
        globalDeclarationTable: GlobalDeclarationTable,
        startIndex: Long
) : DeclarationTable(descriptorTable, globalDeclarationTable, startIndex),
        DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware {

    /**
     * It is incorrect to compute UniqId for declarations from metadata-based libraries.
     * Instead we should get precomputed value from metadata.
     */
    override fun tryComputeBackendSpecificUniqId(declaration: IrDeclaration): UniqId? {
        return if (declaration.descriptor.module.isFromInteropLibrary()) {
            UniqId(declaration.descriptor.getUniqId() ?: error("No uniq id found for ${declaration.descriptor}"))
        } else {
            null
        }
    }
}

class KonanIrModuleSerializer(
    logger: LoggingContext,
    irBuiltIns: IrBuiltIns,
    private val descriptorTable: DescriptorTable
) : IrModuleSerializer<KonanIrFileSerializer>(logger) {


    private val globalDeclarationTable = KonanGlobalDeclarationTable(irBuiltIns)

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer =
            KonanIrFileSerializer(logger, KonanDeclarationTable(descriptorTable, globalDeclarationTable, 0))

}