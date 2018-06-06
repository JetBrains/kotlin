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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtCallableDeclaration

interface KotlinMethodDescriptor : MethodDescriptor<KotlinParameterInfo, Visibility> {
    enum class Kind(val isConstructor: Boolean) {
        FUNCTION(false),
        PRIMARY_CONSTRUCTOR(true),
        SECONDARY_CONSTRUCTOR(true)
    }

    val kind: Kind get() {
        val descriptor = baseDescriptor
        return when {
            descriptor !is ConstructorDescriptor -> Kind.FUNCTION
            descriptor.isPrimary -> Kind.PRIMARY_CONSTRUCTOR
            else -> Kind.SECONDARY_CONSTRUCTOR
        }
    }

    val original: KotlinMethodDescriptor

    val baseDeclaration: PsiElement
    val baseDescriptor: CallableDescriptor

    val originalPrimaryCallable: KotlinCallableDefinitionUsage<PsiElement>
    val primaryCallables: Collection<KotlinCallableDefinitionUsage<PsiElement>>
    val affectedCallables: Collection<UsageInfo>

    val receiver: KotlinParameterInfo?
}

val KotlinMethodDescriptor.returnTypeInfo: KotlinTypeInfo
    get() {
        val type = baseDescriptor.returnType
        val text = (baseDeclaration as? KtCallableDeclaration)?.typeReference?.text
                   ?: type?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type) }
                   ?: "Unit"
        return KotlinTypeInfo(true, type, text)
    }

val KotlinMethodDescriptor.receiverTypeInfo: KotlinTypeInfo
    get() {
        val type = baseDescriptor.extensionReceiverParameter?.type
        val text = (baseDeclaration as? KtCallableDeclaration)?.receiverTypeReference?.text
                   ?: type?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type) }
        return KotlinTypeInfo(false, type, text)
    }
