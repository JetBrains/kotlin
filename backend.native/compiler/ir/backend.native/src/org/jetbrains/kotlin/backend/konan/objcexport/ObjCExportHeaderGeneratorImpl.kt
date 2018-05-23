package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.reportCompilationWarning
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.util.report

internal class ObjCExportHeaderGeneratorImpl(val context: Context)
    : ObjCExportHeaderGenerator(context.moduleDescriptor, context.builtIns) {

    override fun reportWarning(text: String) {
        context.reportCompilationWarning(text)
    }

    override fun reportWarning(method: FunctionDescriptor, text: String) {
        context.report(
                context.ir.get(method),
                text,
                isError = false
        )
    }
}