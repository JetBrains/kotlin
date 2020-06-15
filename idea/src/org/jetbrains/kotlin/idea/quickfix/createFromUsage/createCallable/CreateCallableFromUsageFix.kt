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
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.idea.quickfix.KotlinCrossLanguageQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.isAbstract
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import java.util.*

open class CreateCallableFromUsageFix<E : KtElement>(
    originalExpression: E,
    callableInfos: List<CallableInfo>
) : CreateCallableFromUsageFixBase<E>(originalExpression, callableInfos, false)

class CreateExtensionCallableFromUsageFix<E : KtElement>(
    originalExpression: E,
    callableInfos: List<CallableInfo>
) : CreateCallableFromUsageFixBase<E>(originalExpression, callableInfos, true), LowPriorityAction

abstract class CreateCallableFromUsageFixBase<E : KtElement>(
    originalExpression: E,
    protected val callableInfos: List<CallableInfo>,
    val isExtension: Boolean
) : KotlinCrossLanguageQuickFixAction<E>(originalExpression) {
    init {
        assert(callableInfos.isNotEmpty()) { "No CallableInfos: ${originalExpression.getElementTextWithContext()}" }
        if (callableInfos.size > 1) {
            val receiverSet = callableInfos.mapTo(HashSet()) { it.receiverTypeInfo }
            if (receiverSet.size > 1) throw AssertionError("All functions must have common receiver: $receiverSet")

            val possibleContainerSet = callableInfos.mapTo(HashSet()) { it.possibleContainers }
            if (possibleContainerSet.size > 1) throw AssertionError("All functions must have common containers: $possibleContainerSet")
        }
    }

    private fun getDeclaration(descriptor: ClassifierDescriptor, project: Project): PsiElement? {
        if (descriptor is FunctionClassDescriptor) {
            val psiFactory = KtPsiFactory(project)
            val syntheticClass = psiFactory.createClass(IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.render(descriptor))
            return psiFactory.createAnalyzableFile("${descriptor.name.asString()}.kt", "", element!!).add(syntheticClass)
        }
        return DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
    }

    private fun getDeclarationIfApplicable(project: Project, candidate: TypeCandidate): PsiElement? {
        val descriptor = candidate.theType.constructor.declarationDescriptor ?: return null
        val declaration = getDeclaration(descriptor, project) ?: return null
        if (declaration !is KtClassOrObject && declaration !is KtTypeParameter && declaration !is PsiClass) return null
        return if (isExtension || declaration.canRefactor()) declaration else null
    }

    private fun KotlinType.companionObjectTypeOrThis(): KotlinType {
        val classDescriptor = constructor.declarationDescriptor ?: return this
        if (classDescriptor.isCompanionObject()) return this
        val classDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor) as? KtClass ?: return this
        val copyClass = classDeclaration.copy() as KtClass
        val companionObject = copyClass.getOrCreateCompanionObject()
        val newContext = copyClass.analyzeAsReplacement(classDeclaration, classDeclaration.analyze(BodyResolveMode.PARTIAL))
        val newClassDescriptor = newContext[BindingContext.DECLARATION_TO_DESCRIPTOR, companionObject] as? ClassDescriptor ?: return this
        return newClassDescriptor.classValueType ?: this
    }

    override fun getFamilyName(): String = KotlinBundle.message("fix.create.from.usage.family")

    override fun getText(): String {
        val element = element ?: return ""
        val callableInfo = callableInfos.first()
        val receiverTypeInfo = callableInfo.receiverTypeInfo
        val renderedCallables = callableInfos.map {
            buildString {
                if (it.isAbstract) {
                    append(KotlinBundle.message("text.abstract"))
                    append(' ')
                }

                val kind = when (it.kind) {
                    CallableKind.FUNCTION -> KotlinBundle.message("text.function")
                    CallableKind.PROPERTY -> KotlinBundle.message("text.property")
                    CallableKind.CONSTRUCTOR -> KotlinBundle.message("text.secondary.constructor")
                    else -> throw AssertionError("Unexpected callable info: $it")
                }
                append(kind)

                if (it.name.isNotEmpty()) {
                    append(" '")

                    val receiverType = if (!receiverTypeInfo.isOfThis) {
                        CallableBuilderConfiguration(callableInfos, element, isExtension = isExtension)
                            .createBuilder()
                            .computeTypeCandidates(receiverTypeInfo)
                            .firstOrNull()
                            ?.theType
                            ?.let { if (callableInfo.isForCompanion) it.companionObjectTypeOrThis() else it }
                    } else null

                    if (receiverType != null) {
                        if (isExtension) {
                            val receiverTypeText = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(receiverType)
                            val isFunctionType = receiverType.constructor.declarationDescriptor is FunctionClassDescriptor
                            append(if (isFunctionType) "($receiverTypeText)" else receiverTypeText).append('.')
                        } else {
                            receiverType.constructor.declarationDescriptor?.let {
                                append(IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderClassifierName(it)).append('.')
                            }
                        }
                    }

                    append("${it.name}'")
                }
            }
        }

        return StringBuilder().apply {
            append(KotlinBundle.message("text.create"))
            append(' ')

            if (!callableInfos.any { it.isAbstract }) {
                if (isExtension) {
                    append(KotlinBundle.message("text.extension"))
                    append(' ')
                } else if (receiverTypeInfo != TypeInfo.Empty) {
                    append(KotlinBundle.message("text.member"))
                    append(' ')
                }
            }

            renderedCallables.joinTo(this)
        }.toString()
    }

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false

        val receiverInfo = callableInfos.first().receiverTypeInfo

        if (receiverInfo == TypeInfo.Empty) {
            if (callableInfos.any { it is PropertyInfo && it.possibleContainers.isEmpty() }) return false
            return !isExtension
        }
        // TODO: Remove after companion object extensions are supported
        if (isExtension && receiverInfo.staticContextRequired) return false

        val callableBuilder = CallableBuilderConfiguration(callableInfos, element, isExtension = isExtension).createBuilder()
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
                !isExtension && declaration is KtTypeParameter -> false
                propertyInfo != null && !propertyInfo.isAbstract && declaration is KtClass && declaration.isInterface() -> false
                else ->
                    declaration != null
            }
        }
    }

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        val element = element ?: return
        val callableInfo = callableInfos.first()

        val fileForBuilder = element.containingKtFile

        val editorForBuilder = EditorHelper.openInEditor(element)
        if (editorForBuilder != editor) {
            NavigationUtil.activateFileWithPsiElement(element)
        }

        val callableBuilder =
            CallableBuilderConfiguration(callableInfos, element as KtElement, fileForBuilder, editorForBuilder, isExtension).createBuilder()

        fun runBuilder(placement: () -> CallablePlacement) {
            project.executeCommand(text) {
                callableBuilder.placement = placement()
                callableBuilder.build()
            }
        }

        if (callableInfo is ConstructorInfo) {
            runBuilder { CallablePlacement.NoReceiver(callableInfo.targetClass) }
        }

        val popupTitle = KotlinBundle.message("choose.target.class.or.interface")
        val receiverTypeInfo = callableInfo.receiverTypeInfo
        val receiverTypeCandidates = callableBuilder.computeTypeCandidates(receiverTypeInfo).let {
            if (callableInfo.isAbstract)
                it.filter { it.theType.isAbstract() }
            else if (!isExtension && receiverTypeInfo != TypeInfo.Empty)
                it.filter { !it.theType.isTypeParameter() }
            else
                it
        }
        if (receiverTypeCandidates.isNotEmpty()) {
            val containers = receiverTypeCandidates
                .mapNotNull { candidate -> getDeclarationIfApplicable(project, candidate)?.let { candidate to it } }

            chooseContainerElementIfNecessary(containers, editorForBuilder, popupTitle, false, { it.second }) {
                runBuilder {
                    val receiverClass = it.second as? KtClass
                    if (callableInfo.isForCompanion && receiverClass?.isWritable == true) {
                        val companionObject = receiverClass.getOrCreateCompanionObject()
                        val classValueType = (companionObject.descriptor as? ClassDescriptor)?.classValueType
                        val receiverTypeCandidate = if (classValueType != null) TypeCandidate(classValueType) else it.first
                        CallablePlacement.WithReceiver(receiverTypeCandidate)
                    } else {
                        CallablePlacement.WithReceiver(it.first)
                    }
                }
            }
        } else {
            assert(receiverTypeInfo == TypeInfo.Empty) {
                "No receiver type candidates: ${element.text} in ${file.text}"
            }

            chooseContainerElementIfNecessary(callableInfo.possibleContainers, editorForBuilder, popupTitle, true, { it }) {
                val container = if (it is KtClassBody) it.parent as KtClassOrObject else it
                runBuilder { CallablePlacement.NoReceiver(container) }
            }
        }
    }
}
