/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.org.objectweb.asm.Type

val recordNamesForKotlinTypeMapperPhase = makeIrFilePhase<JvmBackendContext>(
    { context -> RecordNamesForKotlinTypeMapper(context) },
    name = "RecordNamesForKotlinTypeMapper",
    description = "Record local class and anonymous object names for KotlinTypeMapper to work correctly"
)

class RecordNamesForKotlinTypeMapper(private val context: JvmBackendContext) : FileLoweringPass, IrElementVisitorVoid {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        val internalName = context.getLocalClassInfo(declaration)?.internalName
        if (internalName != null) {
            // If this line fails, it means that the name invented by the JVM IR backend in InventNamesForLocalClasses is not equal
            // to the name invented by the old backend in CodegenAnnotatingVisitor. The former should likely be fixed.
            context.state.bindingTrace.record(CodegenBinding.ASM_TYPE, declaration.symbol.descriptor, Type.getObjectType(internalName))
        }
        super.visitClass(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}
