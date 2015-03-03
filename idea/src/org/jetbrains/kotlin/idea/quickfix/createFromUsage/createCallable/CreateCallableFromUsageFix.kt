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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.JetBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.psi.JetExpression
import java.util.HashSet
import org.jetbrains.kotlin.psi.JetElement
import com.intellij.psi.PsiClass
import java.util.Collections
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import com.intellij.psi.PsiElement

public class CreateCallableFromUsageFix(
        originalExpression: JetExpression,
        val callableInfos: List<CallableInfo>,
        val isExtension: Boolean
) : CreateFromUsageFixBase(originalExpression) {
    {
        assert (callableInfos.isNotEmpty(), "No CallableInfos: ${JetPsiUtil.getElementTextWithContext(originalExpression)}")
        if (callableInfos.size > 1) {
            val receiverSet = callableInfos.mapTo(HashSet<TypeInfo>()) { it.receiverTypeInfo }
            if (receiverSet.size > 1) throw AssertionError("All functions must have common receiver: $receiverSet")

            val possibleContainerSet = callableInfos.mapTo(HashSet<List<JetElement>>()) { it.possibleContainers }
            if (possibleContainerSet.size > 1) throw AssertionError("All functions must have common containers: $possibleContainerSet")
        }
    }

    private fun getDeclarationIfApplicable(project: Project, candidate: TypeCandidate): PsiElement? {
        val descriptor = candidate.theType.getConstructor().getDeclarationDescriptor()
        val declaration = DescriptorToDeclarationUtil.getDeclaration(project, descriptor) ?: return null
        if (declaration !is JetClassOrObject && declaration !is PsiClass) return null
        return if (isExtension || declaration.canRefactor()) declaration else null
    }

    override fun getText(): String {
        val renderedCallables = callableInfos.map {
            val kind = when (it.kind) {
                CallableKind.FUNCTION -> "function"
                CallableKind.PROPERTY -> "property"
                else -> throw AssertionError("Unexpected callable info: $it")
            }
            "$kind '${it.name}'"
        }
        return JetBundle.message(
                "create.0.from.usage",
                renderedCallables.joinToString(prefix = if (isExtension) "extension " else "")
        )
    }

    fun isAvailable(): Boolean {
        val callableInfo = callableInfos.first()
        val receiverInfo = callableInfo.receiverTypeInfo

        if (receiverInfo is TypeInfo.Empty) return !isExtension
        // TODO: Remove after default object extensions are supported
        if (isExtension && receiverInfo.staticContextRequired) return false

        val file = element.getContainingFile() as JetFile
        val project = file.getProject()
        val callableBuilder =
                CallableBuilderConfiguration(callableInfos, element as JetExpression, file, null, isExtension).createBuilder()
        val receiverTypeCandidates = callableBuilder.computeTypeCandidates(callableInfos.first().receiverTypeInfo)
        val propertyInfo = callableInfos.firstOrNull { it is PropertyInfo } as PropertyInfo?
        val isFunction = callableInfos.any { it.kind == CallableKind.FUNCTION }
        return receiverTypeCandidates.any {
            val declaration = getDeclarationIfApplicable(project, it)
            val insertToJavaInterface = declaration is PsiClass && declaration.isInterface()
            when {
                propertyInfo != null && insertToJavaInterface && (!receiverInfo.staticContextRequired || propertyInfo.writable) ->
                    false
                isFunction && insertToJavaInterface && receiverInfo.staticContextRequired ->
                    false
                else ->
                    declaration != null
            }
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        val callableInfo = callableInfos.first()

        val callableBuilder =
                CallableBuilderConfiguration(callableInfos, element as JetExpression, file!!, editor!!, isExtension).createBuilder()

        fun runBuilder(placement: CallablePlacement) {
            callableBuilder.placement = placement
            CommandProcessor.getInstance().executeCommand(project, { callableBuilder.build() }, getText(), null)
        }

        val popupTitle = JetBundle.message("choose.target.class.or.trait.title")
        val receiverTypeCandidates = callableBuilder.computeTypeCandidates(callableInfo.receiverTypeInfo)
        if (receiverTypeCandidates.isNotEmpty()) {
            val containers = receiverTypeCandidates
                    .map { candidate -> getDeclarationIfApplicable(project, candidate)?.let { candidate to it } }
                    .filterNotNull()

            chooseContainerElementIfNecessary(containers, editor, popupTitle, false, { it.second }) {
                runBuilder(CallablePlacement.WithReceiver(it.first))
            }
        }
        else {
            assert(callableInfo.receiverTypeInfo is TypeInfo.Empty, "No receiver type candidates: ${element.getText()} in ${file.getText()}")

            chooseContainerElementIfNecessary(callableInfo.possibleContainers, editor, popupTitle, true, { it }) {
                val container = if (it is JetClassBody) it.getParent() as JetClassOrObject else it
                runBuilder(CallablePlacement.NoReceiver(container))
            }
        }
    }
}

public fun CreateCallableFromUsageFixes(
        originalExpression: JetExpression,
        callableInfos: List<CallableInfo>
) : List<CreateCallableFromUsageFix> {
    return listOf(
            CreateCallableFromUsageFix(originalExpression, callableInfos, false),
            CreateCallableFromUsageFix(originalExpression, callableInfos, true)
    ).filter { it.isAvailable() }
}

public fun CreateCallableFromUsageFixes(
        originalExpression: JetExpression,
        callableInfo: CallableInfo
) : List<CreateCallableFromUsageFix> {
    return CreateCallableFromUsageFixes(originalExpression, Collections.singletonList(callableInfo))
}
