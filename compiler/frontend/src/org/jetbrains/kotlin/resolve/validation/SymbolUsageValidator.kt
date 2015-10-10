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

package org.jetbrains.kotlin.resolve.validation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

public interface SymbolUsageValidator {

    public fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) { }

    public fun validateCall(resolvedCall: ResolvedCall<*>?, targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) { }

    public open class Composite(val validators: List<SymbolUsageValidator>) : SymbolUsageValidator {
        override fun validateCall(resolvedCall: ResolvedCall<*>?, targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
            validators.forEach { it.validateCall(resolvedCall, targetDescriptor, trace, element) }
        }

        override fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) {
            validators.forEach { it.validateTypeUsage(targetDescriptor, trace, element) }
        }
    }

    companion object {
        val Empty: SymbolUsageValidator = Composite(listOf())
    }
}