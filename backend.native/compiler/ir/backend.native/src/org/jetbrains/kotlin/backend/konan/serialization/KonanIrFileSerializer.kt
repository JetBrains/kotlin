package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation

class KonanIrFileSerializer(
        logger: LoggingContext,
        declarationTable: DeclarationTable,
        bodiesOnlyForInlines: Boolean = false
): IrFileSerializer(logger, declarationTable, bodiesOnlyForInlines) {

    override fun backendSpecificExplicitRoot(declaration: IrFunction) =
            declaration.annotations.hasAnnotation(RuntimeNames.exportForCppRuntime)

    override fun backendSpecificExplicitRoot(declaration: IrClass) =
            declaration.annotations.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation)
}