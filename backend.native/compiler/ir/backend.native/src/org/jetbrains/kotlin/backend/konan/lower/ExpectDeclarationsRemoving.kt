package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.ir.declarations.IrFile

/**
 * This pass removes all declarations with `isExpect == true`.
 */
internal class ExpectDeclarationsRemoving(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        // All declarations with `isExpect == true` are nested into a top-level declaration with `isExpect == true`.
        irFile.declarations.removeAll { it.descriptor.isExpectMember }
    }
}
