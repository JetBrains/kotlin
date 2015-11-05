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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext

data class CreateParameterData<E : KtElement>(
        val context: BindingContext,
        val parameterInfo: KotlinParameterInfo,
        val originalExpression: E
)

abstract class CreateParameterFromUsageFactory<E : KtElement>: KotlinSingleIntentionActionFactoryWithDelegate<E, CreateParameterData<E>>() {
    override fun createFix(data: CreateParameterData<E>): IntentionAction? {
        return CreateParameterFromUsageFix(
                data.parameterInfo.callableDescriptor as FunctionDescriptor,
                data.parameterInfo,
                data.originalExpression)
    }
}