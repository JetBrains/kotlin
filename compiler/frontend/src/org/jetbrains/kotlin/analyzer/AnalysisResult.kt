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
import org.jetbrains.kotlin.types.ErrorUtils
import kotlin.platform.platformStatic

public data open class AnalysisResult protected (
        public val bindingContext: BindingContext,
        public val moduleDescriptor: ModuleDescriptor
) {

    public val error: Throwable
        get() = if (this is Error) this.exception else throw IllegalStateException("Should only be called for error analysis result")

    public fun isError(): Boolean = this is Error

    public fun throwIfError() {
        if (isError()) {
            throw IllegalStateException("failed to analyze: " + error, error)
        }
    }

    private class Error(bindingContext: BindingContext, val exception: Throwable) : AnalysisResult(bindingContext, ErrorUtils.getErrorModule())

    default object {
        public val EMPTY: AnalysisResult = success(BindingContext.EMPTY, ErrorUtils.getErrorModule())

        platformStatic public fun success(bindingContext: BindingContext, module: ModuleDescriptor): AnalysisResult {
            return AnalysisResult(bindingContext, module)
        }

        platformStatic public fun error(bindingContext: BindingContext, error: Throwable): AnalysisResult {
            return Error(bindingContext, error)
        }
    }
}
