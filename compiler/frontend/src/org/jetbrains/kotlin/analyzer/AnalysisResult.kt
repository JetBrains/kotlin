/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.error.ErrorUtils
import java.io.File

open class AnalysisResult protected constructor(
    val bindingContext: BindingContext,
    val moduleDescriptor: ModuleDescriptor,
    val shouldGenerateCode: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is AnalysisResult && bindingContext == other.bindingContext &&
                moduleDescriptor == other.moduleDescriptor && shouldGenerateCode == other.shouldGenerateCode)
    }

    override fun hashCode(): Int {
        var result = 17
        result = 29 * result + bindingContext.hashCode()
        result = 29 * result + moduleDescriptor.hashCode()
        result = 29 * result + shouldGenerateCode.hashCode()
        return result
    }

    operator fun component1() = bindingContext

    operator fun component2() = moduleDescriptor

    operator fun component3() = shouldGenerateCode

    val error: Throwable
        get() = if (this is InternalError) this.exception else throw IllegalStateException("Should only be called for error analysis result")

    fun isError(): Boolean = this is InternalError || this is CompilationError

    fun throwIfError() {
        when (this) {
            is InternalError -> throw IllegalStateException("failed to analyze: $error", error)
            is CompilationError -> throw CompilationErrorException()
        }
    }

    private class CompilationError(bindingContext: BindingContext) : AnalysisResult(bindingContext, ErrorUtils.errorModule)

    private class InternalError(
        bindingContext: BindingContext,
        val exception: Throwable
    ) : AnalysisResult(bindingContext, ErrorUtils.errorModule)

    class RetryWithAdditionalRoots(
        bindingContext: BindingContext,
        moduleDescriptor: ModuleDescriptor,
        val additionalJavaRoots: List<File>,
        val additionalKotlinRoots: List<File>,
        val additionalClassPathRoots: List<File> = emptyList(),
        val addToEnvironment: Boolean = true
    ) : AnalysisResult(bindingContext, moduleDescriptor)

    companion object {
        val EMPTY: AnalysisResult = success(BindingContext.EMPTY, ErrorUtils.errorModule)

        @JvmStatic
        fun success(bindingContext: BindingContext, module: ModuleDescriptor): AnalysisResult {
            return AnalysisResult(bindingContext, module, true)
        }

        @JvmStatic
        fun success(bindingContext: BindingContext, module: ModuleDescriptor, shouldGenerateCode: Boolean): AnalysisResult {
            return AnalysisResult(bindingContext, module, shouldGenerateCode)
        }

        @JvmStatic
        fun internalError(bindingContext: BindingContext, error: Throwable): AnalysisResult {
            return InternalError(bindingContext, error)
        }

        @JvmStatic
        fun compilationError(bindingContext: BindingContext): AnalysisResult {
            return CompilationError(bindingContext)
        }
    }
}
