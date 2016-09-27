package org.jetbrains.kotlin.bc.analyze

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import kotlin.reflect.jvm.internal.impl.descriptors.PackagePartProvider


fun analyze(moduleContext: ModuleContext,
            files:Collection<KtFile>,
            trace:BindingTrace,
            configuration: CompilerConfiguration,
            packagePartProvider: PackagePartProvider) : AnalysisResult {
    val bindingContext = trace.bindingContext
    val module = moduleContext.module
    return AnalysisResult.success(bindingContext, module)
}
