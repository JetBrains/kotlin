/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.analyzer

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.File

open class WebAnalysisResult(
        val bindingTrace: BindingTrace,
        moduleDescriptor: ModuleDescriptor,
        shouldGenerateCode: Boolean
) : AnalysisResult(bindingTrace.bindingContext, moduleDescriptor, shouldGenerateCode) {

    companion object {
        @JvmStatic fun success(trace: BindingTrace, module: ModuleDescriptor): WebAnalysisResult {
            return WebAnalysisResult(trace, module, true)
        }

        @JvmStatic fun success(trace: BindingTrace, module: ModuleDescriptor, shouldGenerateCode: Boolean): WebAnalysisResult {
            return WebAnalysisResult(trace, module, shouldGenerateCode)
        }
    }

    class RetryWithAdditionalRoots(
        bindingTrace: BindingTrace,
        moduleDescriptor: ModuleDescriptor,
        val additionalKotlinRoots: List<File>,
    ) : WebAnalysisResult(bindingTrace, moduleDescriptor, false)
}
