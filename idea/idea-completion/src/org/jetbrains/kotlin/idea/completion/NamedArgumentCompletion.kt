/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.core.ArgumentPositionData
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

object NamedArgumentCompletion {
    fun isOnlyNamedArgumentExpected(nameExpression: KtSimpleNameExpression): Boolean {
        val thisArgument = nameExpression.parent as? KtValueArgument ?: return false
        if (thisArgument.isNamed()) return false

        val callElement = thisArgument.getStrictParentOfType<KtCallElement>() ?: return false

        return callElement.valueArguments.takeWhile { it != thisArgument }.any { it.isNamed() }
    }

    fun complete(collector: LookupElementsCollector, expectedInfos: Collection<ExpectedInfo>, callType: CallType<*>) {
        if (callType != CallType.DEFAULT) return

        val nameToParameterType = HashMap<Name, MutableSet<KotlinType>>()
        for (expectedInfo in expectedInfos) {
            val argumentData = expectedInfo.additionalData as? ArgumentPositionData.Positional ?: continue
            for (parameter in argumentData.namedArgumentCandidates) {
                nameToParameterType.getOrPut(parameter.name) { HashSet() }.add(parameter.type)
            }
        }

        for ((name, types) in nameToParameterType) {
            val typeText = types.singleOrNull()?.let { BasicLookupElementFactory.SHORT_NAMES_RENDERER.renderType(it) } ?: "..."
            val nameString = name.asString()
            val lookupElement = LookupElementBuilder.create("$nameString =")
                .withPresentableText("$nameString =")
                .withTailText(" $typeText")
                .withIcon(KotlinIcons.PARAMETER)
                .withInsertHandler(NamedArgumentInsertHandler(name))
            lookupElement.putUserData(SmartCompletionInBasicWeigher.NAMED_ARGUMENT_KEY, Unit)
            collector.addElement(lookupElement)
        }
    }

    private class NamedArgumentInsertHandler(private val parameterName: Name) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val editor = context.editor
            val text = parameterName.render()
            editor.document.replaceString(context.startOffset, context.tailOffset, text)
            editor.caretModel.moveToOffset(context.startOffset + text.length)

            WithTailInsertHandler.EQ.postHandleInsert(context, item)
        }
    }
}
