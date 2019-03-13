package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.UniqId
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class JsDeclarationTable(builtIns: IrBuiltIns, descriptorTable: DescriptorTable)
    : DeclarationTable(builtIns, descriptorTable, JsMangler) {

    override var currentIndex = 0x1_0000_0000L

    private /* lateinit */ var FUNCTION_INDEX_START: Long = 0xdeadbeefL

    override fun loadKnownBuiltins() {
        super.loadKnownBuiltins()
        FUNCTION_INDEX_START = currentIndex
        currentIndex += BUILT_IN_UNIQ_ID_GAP
    }

    override fun computeUniqIdByDeclaration(value: IrDeclaration) =
        if (isBuiltInFunction(value)) {
            UniqId(FUNCTION_INDEX_START + builtInFunctionId(value), false)
        } else super.computeUniqIdByDeclaration(value)
}