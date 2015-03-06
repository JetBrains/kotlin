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

import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

public object CreateParameterByNamedArgumentActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val result = (diagnostic.getPsiFile() as? JetFile)?.analyzeFullyAndGetResult() ?: return null
        val context = result.bindingContext

        val argument = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetValueArgument>()) ?: return null
        val name = argument.getArgumentName()?.getText() ?: return null
        val argumentExpression = argument.getArgumentExpression()

        val callElement = argument.getStrictParentOfType<JetCallElement>() ?: return null
        val functionDescriptor = callElement.getResolvedCall(context)?.getResultingDescriptor() as? FunctionDescriptor ?: return null
        val callable = DescriptorToSourceUtilsIde.getAnyDeclaration(callElement.getProject(), functionDescriptor) ?: return null
        if (!((callable is JetFunction || callable is JetClass) && callable.canRefactor())) return null

        val anyType = KotlinBuiltIns.getInstance().getAnyType()
        val paramType = argumentExpression?.guessTypes(context, result.moduleDescriptor)?.let {
            when (it.size()) {
                0 -> anyType
                1 -> it.first()
                else -> return null
            }
        } ?: anyType
        if (paramType.hasTypeParametersToAdd(functionDescriptor, context)) return null

        val parameterInfo = JetParameterInfo(
                name = name,
                type = paramType,
                defaultValueForCall = argumentExpression.getText() ?: ""
        )
        return CreateParameterFromUsageFix(functionDescriptor, context, parameterInfo, argument)
    }
}
