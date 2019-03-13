package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.llvm.KonanMangler
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction

class KonanIrModuleSerializer(
    logger: LoggingContext,
    declarationTable: DeclarationTable,
    bodiesOnlyForInlines: Boolean = false
) : IrModuleSerializer(logger, declarationTable, KonanMangler, bodiesOnlyForInlines) {

    override fun backendSpecificExplicitRoot(declaration: IrFunction) =
        declaration.descriptor.annotations.hasAnnotation(RuntimeNames.exportForCppRuntime) ||
                declaration.descriptor.annotations.hasAnnotation(RuntimeNames.exportForCompilerAnnotation)

    override fun backendSpecificExplicitRoot(declaration: IrClass) =
        declaration.descriptor.annotations.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation)

}