package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable

class JsIrModuleSerializer(
    logger: LoggingContext,
    declarationTable: DeclarationTable,
    bodiesOnlyForInlines: Boolean = false
) : IrModuleSerializer(logger, declarationTable, JsMangler, bodiesOnlyForInlines)