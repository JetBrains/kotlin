/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.MethodDescriptor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JetFunctionDefinitionUsage
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers

public trait JetMethodDescriptor : MethodDescriptor<JetParameterInfo, Visibility> {
    val baseDeclaration: PsiElement
    val baseDescriptor: FunctionDescriptor

    val originalPrimaryFunction: JetFunctionDefinitionUsage<PsiElement>
    val primaryFunctions: Collection<JetFunctionDefinitionUsage<PsiElement>>
    val affectedFunctions: Collection<UsageInfo>

    val receiver: JetParameterInfo?
}

val JetMethodDescriptor.isConstructor: Boolean
    get() = baseDescriptor is ConstructorDescriptor

fun JetMethodDescriptor.renderOriginalReturnType(): String =
        baseDescriptor.getReturnType()?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) } ?: ""

fun JetMethodDescriptor.renderOriginalReceiverType(): String? =
        baseDescriptor.getExtensionReceiverParameter()?.getType()?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) }