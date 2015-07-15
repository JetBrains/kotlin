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
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.getAffectedCallables
import org.jetbrains.kotlin.idea.refactoring.changeSignature.isInsideOfCallerBody
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList

public class KotlinCallerUsage(element: JetNamedDeclaration): JetUsageInfo<JetNamedDeclaration>(element) {
    override fun processUsage(changeInfo: JetChangeInfo, element: JetNamedDeclaration, allUsages: Array<out UsageInfo>): Boolean {
        // Do not process function twice
        if (changeInfo.getAffectedCallables().any { it is JetCallableDefinitionUsage<*> && it.getElement() == element }) return true

        val parameterList = when (element) {
            is JetFunction -> element.getValueParameterList()
            is JetClass -> element.createPrimaryConstructorParameterListIfAbsent()
            else -> null
        } ?: return true
        val psiFactory = JetPsiFactory(getProject())
        changeInfo.getNonReceiverParameters()
                .withIndex()
                .filter { it.value.isNewParameter }
                .forEach {
                    val parameterText = it.value.getDeclarationSignature(it.index, changeInfo.methodDescriptor.originalPrimaryCallable)
                    parameterList
                            .addParameter(psiFactory.createParameter(parameterText))
                            .addToShorteningWaitSet()
                }

        return true
    }
}

public class KotlinCallerCallUsage(element: JetCallElement): JetUsageInfo<JetCallElement>(element) {
    override fun processUsage(changeInfo: JetChangeInfo, element: JetCallElement, allUsages: Array<out UsageInfo>): Boolean {
        val argumentList = element.getValueArgumentList() ?: return true
        val psiFactory = JetPsiFactory(getProject())
        val isNamedCall = argumentList.getArguments().any { it.getArgumentName() != null }
        changeInfo.getNonReceiverParameters()
                .filter { it.isNewParameter }
                .forEach {
                    val parameterName = it.getName()
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