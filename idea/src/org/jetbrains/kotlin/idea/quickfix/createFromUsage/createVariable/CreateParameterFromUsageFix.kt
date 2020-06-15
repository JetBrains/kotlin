/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableBuilderConfiguration
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallablePlacement
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.PropertyInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.createBuilder
import org.jetbrains.kotlin.idea.refactoring.CompositeRefactoringRunner
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.source.getPsi

open class CreateParameterFromUsageFix<E : KtElement>(
    val data: CreateParameterData<E>
) : CreateFromUsageFixBase<E>(data.originalExpression) {
    override fun getText(): String {
        return with(data.parameterInfo) {
            if (valOrVar != KotlinValVar.None)
                KotlinBundle.message("create.property.0.as.constructor.parameter", name)
            else
                KotlinBundle.message("create.parameter.0", name)
        }
    }

    override fun startInWriteAction() = false

    private fun runChangeSignature(project: Project) {
        val config = object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                return originalDescriptor.modify { it.addParameter(data.parameterInfo) }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = data.createSilently
        }

        runChangeSignature(project, data.parameterInfo.callableDescriptor, config, data.originalExpression, text)
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val onComplete = data.onComplete
        if (onComplete == null) {
            runChangeSignature(project)
        } else {
            object : CompositeRefactoringRunner(project, "refactoring.changeSignature") {
                override fun runRefactoring() {
                    runChangeSignature(project)
                }

                override fun onRefactoringDone() {
                    onComplete(editor)
                }
            }.run()
        }
    }

    companion object {
        fun <E : KtElement> createFixForPrimaryConstructorPropertyParameter(
            element: E,
            info: PropertyInfo
        ): CreateParameterFromUsageFix<E>? {
            if (info.isForCompanion) return null

            val receiverClassDescriptor: ClassDescriptor

            val builder = CallableBuilderConfiguration(listOf(info), element).createBuilder()
            val receiverTypeCandidate = builder.computeTypeCandidates(info.receiverTypeInfo).firstOrNull()
            if (receiverTypeCandidate != null) {
                builder.placement = CallablePlacement.WithReceiver(receiverTypeCandidate)
                receiverClassDescriptor = receiverTypeCandidate.theType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
            } else {
                if (element !is KtSimpleNameExpression) return null

                val classOrObject = element.getStrictParentOfType<KtClassOrObject>() ?: return null
                receiverClassDescriptor = classOrObject.resolveToDescriptorIfAny() ?: return null

                val paramInfo = CreateParameterByRefActionFactory.extractFixData(element)?.parameterInfo
                if (paramInfo?.callableDescriptor == receiverClassDescriptor.unsubstitutedPrimaryConstructor) return null
            }

            if (receiverClassDescriptor.kind != ClassKind.CLASS) return null
            val receiverClass = receiverClassDescriptor.source.getPsi() as? KtClass ?: return null
            if (!receiverClass.canRefactor()) return null
            val constructorDescriptor = receiverClassDescriptor.unsubstitutedPrimaryConstructor ?: return null

            val paramType = info.returnTypeInfo.getPossibleTypes(builder).firstOrNull()
            if (paramType != null && paramType.hasTypeParametersToAdd(constructorDescriptor, builder.currentFileContext)) return null

            val paramInfo = KotlinParameterInfo(
                callableDescriptor = constructorDescriptor,
                name = info.name,
                originalTypeInfo = KotlinTypeInfo(false, paramType),
                valOrVar = if (info.writable) KotlinValVar.Var else KotlinValVar.Val
            )

            return CreateParameterFromUsageFix(CreateParameterData(paramInfo, element))
        }
    }
}