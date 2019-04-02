package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns


// TODO: We don't manage id clashes anyhow now.
class KonanDeclarationTable(builtIns: IrBuiltIns, descriptorTable: DescriptorTable):
    DeclarationTable(builtIns, descriptorTable, KonanMangler) {
    override var currentIndex = 0L

    init {
        loadKnownBuiltins()
    }
}
