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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableBuilderConfiguration
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallablePlacement
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.PropertyInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.createBuilder
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.source.getPsi

public open class CreateParameterFromUsageFix<E : KtElement>(
        val functionDescriptor: FunctionDescriptor,
        val parameterInfo: KotlinParameterInfo,
        val defaultValueContext: E
) : CreateFromUsageFixBase<E>(defaultValueContext) {
    override fun getText(): String {
        return with(parameterInfo) {
            if (valOrVar != KotlinValVar.None) "Create property '$name' as constructor parameter" else "Create parameter '$name'"
        }
    }

    override fun startInWriteAction() = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val config = object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                return originalDescriptor.modify { it.addParameter(parameterInfo) }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = false
        }

        runChangeSignature(project, functionDescriptor, config, defaultValueContext, text)
    }

    companion object {
        public fun <E : KtElement> createFixForPrimaryConstructorPropertyParameter(
                element: E,
                info: PropertyInfo
        ) : CreateParameterFromUsageFix<E>? {
            val receiverClassDescriptor: ClassDescriptor

            val builder = CallableBuilderConfiguration(listOf(info), element, element.getContainingKtFile(), null, false).createBuilder()
            val receiverTypeCandidate = builder.computeTypeCandidates(info.receiverTypeInfo).firstOrNull()
            if (receiverTypeCandidate != null) {
                builder.placement = CallablePlacement.WithReceiver(receiverTypeCandidate)
                receiverClassDescriptor = receiverTypeCandidate.theType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
            }
            else {
                if (element !is KtSimpleNameExpression) return null

                val classOrObject = element.getStrictParentOfType<KtClassOrObject>() ?: return null
                receiverClassDescriptor = classOrObject.resolveToDescriptorIfAny() as? ClassDescriptor ?: return null

                val paramInfo = CreateParameterByRefActionFactory.extractFixData(element)?.parameterInfo
                if (paramInfo?.callableDescriptor == receiverClassDescriptor.unsubstitutedPrimaryConstructor) return null
            }

            if (receiverClassDescriptor.kind != ClassKind.CLASS) return null
            val receiverClass = receiverClassDescriptor.source.getPsi() as? KtClass ?: return null
            if (!receiverClass.canRefactor()) return null
            val constructorDescriptor = receiverClassDescriptor.unsubstitutedPrimaryConstructor ?: return null

            val paramType = info.returnTypeInfo.getPossibleTypes(builder).firstOrNull() ?: return null
            if (paramType.hasTypeParametersToAdd(constructorDescriptor, builder.currentFileContext)) return null

            val paramInfo = KotlinParameterInfo(
                    callableDescriptor = constructorDescriptor,
                    name = info.name,
                    type = paramType,
                    valOrVar = if (info.writable) KotlinValVar.Var else KotlinValVar.Val
            )

            return CreateParameterFromUsageFix(constructorDescriptor, paramInfo, element)
        }
    }
}