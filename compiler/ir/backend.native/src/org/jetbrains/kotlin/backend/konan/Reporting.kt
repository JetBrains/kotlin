/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.render

fun ErrorReportingContext.reportCompilationError(message: String, compilerMessageLocation: CompilerMessageLocation?): Nothing {
    diagnosticReporter.report(NativeBackendDiagnostics.NATIVE_BACKEND_ERROR, message, compilerMessageLocation)
    throw KonanCompilationException()
}

fun ErrorReportingContext.reportCompilationError(message: String, irFile: IrFile, irElement: IrElement): Nothing {
    diagnosticReporter.report(NativeBackendDiagnostics.NATIVE_BACKEND_ERROR, message, irElement.getCompilerMessageLocation(irFile))
    throw KonanCompilationException()
}

fun ErrorReportingContext.reportCompilationError(message: String): Nothing {
    diagnosticReporter.report(NativeBackendDiagnostics.NATIVE_BACKEND_ERROR, message)
    throw KonanCompilationException()
}

fun error(irFile: IrFile?, element: IrElement?, message: String): Nothing {
    error(renderCompilerError(irFile, element, message))
}

fun renderCompilerError(irFile: IrFile?, element: IrElement?, message: String) =
        buildString {
            append("Internal compiler error: $message\n")
            if (element == null) {
                append("(IR element is null)")
            } else {
                if (irFile != null) {
                    val location = element.getCompilerMessageLocation(irFile)
                    append("at $location\n")
                }

                val renderedElement = try {
                    element.render()
                } catch (e: Throwable) {
                    "(unable to render IR element)"
                }
                append(renderedElement)
            }
        }
