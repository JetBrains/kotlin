/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.test.backend.handlers.SerializedIrDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class JKlibSerializedIrDumpHandler(
    testServices: TestServices,
    isAfterDeserialization: Boolean,
) : SerializedIrDumpHandler(testServices, isAfterDeserialization) {

    override fun createDumpOptions(
        module: TestModule,
        info: IrBackendInput,
        isFirFrontend: Boolean,
    ): DumpIrTreeOptions {
        return super.createDumpOptions(module, info, isFirFrontend).copy(
            renderOriginForExternalDeclarations = false,
            printSealedSubclasses = false,
        )
    }
}
