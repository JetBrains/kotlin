/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddNamesToCallArgumentsIntention : SelfTargetingRangeIntention<KtCallElement>(
        KtCallElement::class.java,
        "Add names to call arguments"
) {
    override fun applicabilityRange(element: KtCallElement): TextRange? {
        val arguments = element.valueArguments
        if (arguments.all { it.isNamed() || it is LambdaArgument }) return null

        val resolvedCall = element.getResolvedCall(element.analyze(BodyResolveMode.PARTIAL)) ?: return null
        if (!resolvedCall.resultingDescriptor.hasStableParameterNames()) return null

        for (argument in arguments) {
            val argumentMatch = resolvedCall.getArgumentMapping(argument) as? ArgumentMatch ?: return null
            if (argumentMatch.status != ArgumentMatchStatus.SUCCESS) return null

            if (argumentMatch.valueParameter.varargElementType != null) {
                val varargArgument = resolvedCall.valueArguments[argumentMatch.valueParameter] as? VarargValueArgument ?: return null
                if (varargArgument.arguments.size != 1) return null
            }
        }

        return element.calleeExpression?.textRange
    }

    override fun applyTo(element: KtCallElement, editor: Editor?) {
        val arguments = element.valueArguments
        val resolvedCall = element.getResolvedCall(element.analyze(BodyResolveMode.PARTIAL)) ?: return
        for (argument in arguments) {
            if (argument !is KtValueArgument || argument is KtLambdaArgument) continue
            val argumentMatch = resolvedCall.getArgumentMapping(argument) as? ArgumentMatch ?: continue
            val name = argumentMatch.valueParameter.name
            val newArgument = KtPsiFactory(element).createArgument(argument.getArgumentExpression()!!,
                                                                   name,
                                                                   argument.getSpreadElement() != null)
            argument.replace(newArgument)
        }
    }
}