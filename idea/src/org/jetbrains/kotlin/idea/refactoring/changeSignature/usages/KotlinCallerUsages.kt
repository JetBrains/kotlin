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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.getAffectedCallables
import org.jetbrains.kotlin.idea.refactoring.changeSignature.isInsideOfCallerBody
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

class KotlinCallerUsage(element: KtNamedDeclaration): KotlinUsageInfo<KtNamedDeclaration>(element) {
    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtNamedDeclaration, allUsages: Array<out UsageInfo>): Boolean {
        // Do not process function twice
        if (changeInfo.getAffectedCallables().any { it is KotlinCallableDefinitionUsage<*> && it.element == element }) return true

        val parameterList = when (element) {
            is KtFunction -> element.valueParameterList
            is KtClass -> element.createPrimaryConstructorParameterListIfAbsent()
            else -> null
        } ?: return true
        changeInfo.getNonReceiverParameters()
            .asSequence()
            .withIndex()
            .filter { it.value.isNewParameter }
            .toList()
                .forEach {
                    val newParameter = it.value.getDeclarationSignature(it.index, changeInfo.methodDescriptor.originalPrimaryCallable)
                    parameterList.addParameter(newParameter).addToShorteningWaitSet()
                }

        return true
    }
}

class KotlinCallerCallUsage(element: KtCallElement): KotlinUsageInfo<KtCallElement>(element) {
    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtCallElement, allUsages: Array<out UsageInfo>): Boolean {
        val argumentList = element.valueArgumentList ?: return true
        val psiFactory = KtPsiFactory(project)
        val isNamedCall = argumentList.arguments.any { it.isNamed() }
        changeInfo.getNonReceiverParameters()
                .filter { it.isNewParameter }
                .forEach {
                    val parameterName = it.name
                    val argumentExpression = if (element.isInsideOfCallerBody(allUsages)) {
                        psiFactory.createExpression(parameterName)
                    }
                    else {
                        it.defaultValueForCall ?: psiFactory.createExpression("_")
                    }
                    val argument = psiFactory.createArgument(
                            expression = argumentExpression,
                            name = if (isNamedCall) Name.identifier(parameterName) else null
                    )
                    argumentList.addArgument(argument)
                }

        return true
    }
}
