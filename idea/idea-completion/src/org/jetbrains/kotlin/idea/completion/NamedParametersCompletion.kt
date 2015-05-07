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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.filters.AndFilter
import com.intellij.psi.filters.ClassFilter
import com.intellij.psi.filters.OrFilter
import com.intellij.psi.filters.position.ParentElementFilter
import org.jetbrains.kotlin.core.FirstChildInParentFilter
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.core.mapArgumentsToParameters
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.psi.JetValueArgumentName
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import java.util.*

object NamedParametersCompletion {
    private val positionFilter = AndFilter(
            LeafElementFilter(JetTokens.IDENTIFIER),
            OrFilter(
                    AndFilter(
                            ParentElementFilter(ClassFilter(javaClass<JetValueArgument>()), 2),
                            FirstChildInParentFilter(2)
                    ),
                    ParentElementFilter(ClassFilter(javaClass<JetValueArgumentName>()), 2)
            )
    )

    public fun isOnlyNamedParameterExpected(position: PsiElement): Boolean {
        if (!positionFilter.isAcceptable(position, position)) return false

        val thisArgument = position.getStrictParentOfType<JetValueArgument>()!!

        val callElement = thisArgument.getStrictParentOfType<JetCallElement>() ?: return false

        for (argument in callElement.getValueArguments()) {
            if (argument == thisArgument) break
            if (argument.isNamed()) return true
        }

        return false
    }

    public fun complete(position: PsiElement, collector: LookupElementsCollector, bindingContext: BindingContext) {
        if (!positionFilter.isAcceptable(position, position)) return

        val valueArgument = position.getStrictParentOfType<JetValueArgument>()!!

        val callElement = valueArgument.getStrictParentOfType<JetCallElement>() ?: return
        val callSimpleName = callElement.getCallNameExpression() ?: return

        val callReference = callSimpleName.getReference() as JetReference

        val functionDescriptors = callReference.resolveToDescriptors(bindingContext).map { it as? FunctionDescriptor }.filterNotNull()

        for (funDescriptor in functionDescriptors) {
            if (!funDescriptor.hasStableParameterNames()) continue
            val call = callElement.getCall(bindingContext) ?: continue

            var argumentToParameter = HashMap(call.mapArgumentsToParameters(funDescriptor))
            argumentToParameter.remove(valueArgument)
            val usedParameters = argumentToParameter.values().toSet()

            for (parameter in funDescriptor.getValueParameters()) {
                if (parameter !in usedParameters) {
                    val name = parameter.getName().asString()
                    val lookupElement = LookupElementBuilder.create(name)
                            .withPresentableText("$name =")
                            .withTailText(" ${DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(parameter.getType())}")
                            .withIcon(JetIcons.PARAMETER)
                            .withInsertHandler(NamedParameterInsertHandler(parameter.getName()))
                            .assignPriority(ItemPriority.NAMED_PARAMETER)
                    collector.addElement(lookupElement)
                }
            }
        }
    }

    private class NamedParameterInsertHandler(val parameterName: Name) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val editor = context.getEditor()
            val text = IdeDescriptorRenderers.SOURCE_CODE.renderName(parameterName)
            editor.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), text)
            editor.getCaretModel().moveToOffset(context.getStartOffset() + text.length)

            WithTailInsertHandler.eqTail().postHandleInsert(context, item)
        }
    }
}
