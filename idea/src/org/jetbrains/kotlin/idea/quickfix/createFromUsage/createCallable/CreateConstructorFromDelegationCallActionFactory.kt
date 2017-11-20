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

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.SecondaryConstructorInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.Variance

object CreateConstructorFromDelegationCallActionFactory : CreateCallableMemberFromUsageFactory<KtConstructorDelegationCall>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtConstructorDelegationCall? {
        return diagnostic.psiElement.getStrictParentOfType<KtConstructorDelegationCall>()
    }

    override fun createCallableInfo(element: KtConstructorDelegationCall, diagnostic: Diagnostic): CallableInfo? {
        val calleeExpression = element.calleeExpression ?: return null
        val currentClass = element.getStrictParentOfType<KtClass>() ?: return null

        val project = currentClass.project

        val classDescriptor = currentClass.resolveToDescriptorIfAny() as? ClassDescriptor ?: return null

        val targetClass = if (calleeExpression.isThis) {
            currentClass
        }
        else {
            val superClassDescriptor =
                    DescriptorUtils.getSuperclassDescriptors(classDescriptor).singleOrNull { it.kind == ClassKind.CLASS } ?: return null
            DescriptorToSourceUtilsIde.getAnyDeclaration(project, superClassDescriptor) ?: return null
        }
        if (!(targetClass.canRefactor() && (targetClass is KtClass || targetClass is PsiClass))) return null

        val anyType = classDescriptor.builtIns.nullableAnyType
        val parameters = element.valueArguments.map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.asName?.asString()
            )
        }

        return SecondaryConstructorInfo(parameters, targetClass)
    }
}
