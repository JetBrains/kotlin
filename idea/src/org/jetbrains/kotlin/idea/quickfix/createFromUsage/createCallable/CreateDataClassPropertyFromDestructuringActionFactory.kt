/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.ConstantNode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterData
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterFromUsageFactory
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinTypeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.source.getPsi

object CreateDataClassPropertyFromDestructuringActionFactory : CreateParameterFromUsageFactory<KtDestructuringDeclaration>() {
    override fun getElementOfInterest(diagnostic: Diagnostic) = CreateComponentFunctionActionFactory.getElementOfInterest(diagnostic)

    override fun extractFixData(
        element: KtDestructuringDeclaration,
        diagnostic: Diagnostic
    ): CreateParameterData<KtDestructuringDeclaration>? {
        val diagnosticWithParameters = Errors.COMPONENT_FUNCTION_MISSING.cast(diagnostic)

        val functionName = diagnosticWithParameters.a
        if (!DataClassDescriptorResolver.isComponentLike(functionName)) return null
        val componentNumber = DataClassDescriptorResolver.getComponentIndex(functionName.asString()) - 1

        val targetClassDescriptor = diagnosticWithParameters.b.constructor.declarationDescriptor as? ClassDescriptor ?: return null
        if (!targetClassDescriptor.isData) return null
        val targetClass = targetClassDescriptor.source.getPsi() as? KtClass ?: return null
        val valueParameterCount = targetClass.primaryConstructor?.valueParameters?.size ?: 0
        if (valueParameterCount != componentNumber) return null // TODO: Support addition of multiple parameters
        val constructorDescriptor = targetClassDescriptor.unsubstitutedPrimaryConstructor ?: return null

        val entry = element.entries[componentNumber]
        val paramName = entry.name ?: functionName.asString()
        val paramType = entry.typeReference?.getAbbreviatedTypeOrType(entry.analyze()) ?: targetClassDescriptor.builtIns.anyType

        val parameterInfo = KotlinParameterInfo(
            callableDescriptor = constructorDescriptor,
            name = paramName,
            originalTypeInfo = KotlinTypeInfo(false, paramType),
            valOrVar = KotlinValVar.Val
        )

        return CreateParameterData(parameterInfo, element, createSilently = true) { editor ->
            if (editor == null) return@CreateParameterData
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(targetClass)?.let {
                val constructor = it.primaryConstructor ?: return@let
                val newParameter = constructor.valueParameters.lastOrNull() ?: return@let
                val typeReference = newParameter.typeReference ?: return@let
                val templateBuilder = TemplateBuilderImpl(typeReference)
                templateBuilder.replaceElement(typeReference, ConstantNode(typeReference.text))
                templateBuilder.run(editor, true)
            }
        }
    }
}