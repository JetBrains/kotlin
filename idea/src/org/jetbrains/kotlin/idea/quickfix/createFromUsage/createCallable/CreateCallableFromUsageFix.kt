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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.core.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import java.util.HashSet

public class CreateCallableFromUsageFix<E : JetElement>(
        originalExpression: E,
        callableInfos: List<CallableInfo>
) : CreateCallableFromUsageFixBase<E>(originalExpression, callableInfos, false)

public class CreateExtensionCallableFromUsageFix<E : JetElement>(
        originalExpression: E,
        callableInfos: List<CallableInfo>
) : CreateCallableFromUsageFixBase<E>(originalExpression, callableInfos, true), LowPriorityAction

public abstract class CreateCallableFromUsageFixBase<E : JetElement>(
        originalExpression: E,
        val callableInfos: List<CallableInfo>,
        val isExtension: Boolean
) : CreateFromUsageFixBase<E>(originalExpression) {
    init {
        assert (callableInfos.isNotEmpty()) { "No CallableInfos: ${originalExpression.getElementTextWithContext()}" }
        if (callableInfos.size() > 1) {
            val receiverSet = callableInfos.mapTo(HashSet<TypeInfo>()) { it.receiverTypeInfo }
            if (receiverSet.size() > 1) throw AssertionError("All functions must have common receiver: $receiverSet")

            val possibleContainerSet = callableInfos.mapTo(HashSet<List<JetElement>>()) { it.possibleContainers }
            if (possibleContainerSet.size() > 1) throw AssertionError("All functions must have common containers: $possibleContainerSet")
        }
    }

    private fun getDeclarationIfApplicable(project: Project, candidate: TypeCandidate): PsiElement? {
        val descriptor = candidate.theType.constructor.declarationDescriptor ?: return null
        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) ?: return null
        if (declaration !is JetClassOrObject && declaration !is PsiClass) return null
        return if (isExtension || declaration.canRefactor()) declaration else null
    }

    override fun getText(): String {
        val renderedCallables = callableInfos.map {
            val kind = when (it.kind) {
                CallableKind.FUNCTION -> "function"
                CallableKind.PROPERTY -> "property"
                CallableKind.SECONDARY_CONSTRUCTOR -> "secondary constructor"
                else -> throw AssertionError("Unexpected callable info: $it")
            }
            if (it.name.isNotEmpty()) "$kind '${it.name}'" else kind
        }

        return StringBuilder {
            append("Create ")

            val receiverInfo = callableInfos.first().receiverTypeInfo
            if (isExtension) {
                append("extension ")
            }
            else if (receiverInfo !is TypeInfo.Empty) {
                append("member ")
            }

            renderedCallables.joinTo(this)
        }.toString()
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        if (file !is JetFile) return false

        val receiverInfo = callableInfos.first().receiverTypeInfo

        if (receiverInfo is TypeInfo.Empty) return !isExtension
        // TODO: Remove after companion object extensions are supported
        if (isExtension && receiverInfo.staticContextRequired) return false

        val callableBuilder = CallableBuilderConfiguration(callableInfos, element, file, null, isExtension).createBuilder()
        val receiverTypeCandidates = callableBuilder.computeTypeCandidates(callableInfos.first().receiverTypeInfo)
        val propertyInfo = callableInfos.firstOrNull { it is PropertyInfo } as PropertyInfo?
        val isFunction = callableInfos.any { it.kind == CallableKind.FUNCTION }
        return receiverTypeCandidates.any {
            val declaration = getDeclarationIfApplicable(project, it)
            val insertToJavaInterface = declaration is PsiClass && declaration.isInterface
            when {
                !isExtension && propertyInfo != null && insertToJavaInterface && (!receiverInfo.staticContextRequired || propertyInfo.writable) ->
                    false
                isFunction && insertToJavaInterface && receiverInfo.staticContextRequired ->
                    false
                else ->
                    declaration != null
            }
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val callableInfo = callableInfos.first()

        val callableBuilder =
                CallableBuilderConfiguration(callableInfos, element as JetElement, file, editor!!, isExtension).createBuilder()

        fun runBuilder(placement: CallablePlacement) {
            callableBuilder.placement = placement
            project.executeCommand(text) { callableBuilder.build() }
        }

        if (callableInfo is SecondaryConstructorInfo) {
            runBuilder(CallablePlacement.NoReceiver(callableInfo.targetClass))
            return
        }

        val popupTitle = "Choose target class or interface"
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
            assert(callableInfo.receiverTypeInfo is TypeInfo.Empty) {
                "No receiver type candidates: ${element.text} in ${file.text}"
            }

            chooseContainerElementIfNecessary(callableInfo.possibleContainers, editor, popupTitle, true, { it }) {
                val container = if (it is JetClassBody) it.parent as JetClassOrObject else it
                runBuilder(CallablePlacement.NoReceiver(container))
            }
        }
    }
}
