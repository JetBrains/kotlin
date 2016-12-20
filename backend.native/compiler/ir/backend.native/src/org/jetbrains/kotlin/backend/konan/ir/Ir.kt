package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

// This is what Context collects about IR.
internal class Ir(val context: Context, val irModule: IrModuleFragment) {
    val propertiesWithBackingFields = mutableSetOf<PropertyDescriptor>()

    val moduleIndex = ModuleIndex(irModule)
}
