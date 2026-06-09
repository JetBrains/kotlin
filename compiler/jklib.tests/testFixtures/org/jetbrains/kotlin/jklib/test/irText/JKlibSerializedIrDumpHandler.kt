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

    override fun normalizeDump(dump: String, module: TestModule, info: IrBackendInput): String {
        return dump.normalizeForSerializedIrDump()
    }
}

private val STUB_AND_VARIANCE_REGEX = Regex(
    """\b(?:IR_EXTERNAL_DECLARATION_STUB|IR_EXTERNAL_JAVA_DECLARATION_STUB)\b ?|@\[(?:UnsafeVariance|FlexibleArrayElementVariance)\] ?"""
)

private fun String.normalizeForSerializedIrDump(): String {
    val result = this.replace(STUB_AND_VARIANCE_REGEX, "")

    // Remove enhanced nullability and mutability details on return types in fake overrides.
    val lines = result.split("\n")
    val normalizedLines = lines.map { line ->
        if (line.contains("FUN FAKE_OVERRIDE")) {
            val returnTypeRegex = Regex("""returnType:(.+?)(?=\s+\[|$)""")
            returnTypeRegex.replace(line) { matchResult ->
                val typeStr = matchResult.groupValues[1]
                val normalizedType = typeStr
                    .replace(Regex("""@\[[^\]]+\]\s*"""), "") // Remove annotations
                    .replace(Regex("""<.*>"""), "") // Remove generic arguments
                    .replace(">", "") // Remove raw type trailing >
                    .replace("?", "") // Remove nullability
                "returnType:$normalizedType"
            }
        } else {
            line
        }
    }
    return normalizedLines.joinToString("\n")
}
