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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.kotlin.psi.JetUserType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetDelegatorToSuperCall
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.JetConstructorCalleeExpression
import java.util.Collections
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public object CreateClassFromCallWithConstructorCalleeActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val diagElement = diagnostic.getPsiElement()

        val callElement = PsiTreeUtil.getParentOfType(
                diagElement,
                javaClass<JetAnnotationEntry>(),
                javaClass<JetDelegatorToSuperCall>()
        ) as? JetCallElement ?: return null

        val isAnnotation = callElement is JetAnnotationEntry

        val callee = callElement.getCalleeExpression() as? JetConstructorCalleeExpression ?: return null
        val calleeRef = callee.getConstructorReferenceExpression() ?: return null

        if (!calleeRef.isAncestor(diagElement)) return null

        val file = callElement.getContainingFile() as? JetFile ?: return null
        val typeRef = callee.getTypeReference() ?: return null
        val userType = typeRef.getTypeElement() as? JetUserType ?: return null

        val context = userType.analyze()

        val qualifier = userType.getQualifier()?.getReferenceExpression()
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParent = getTargetParentByQualifier(file, qualifier != null, qualifierDescriptor) ?: return null

        val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()
        val valueArguments = callElement.getValueArguments()
        val defaultParamName = if (valueArguments.size() == 1) "value" else null
        val parameterInfos = valueArguments.map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.asName?.asString() ?: defaultParamName
            )
        }

        val typeArgumentInfos = when {
            isAnnotation -> Collections.emptyList<TypeInfo>()
            else -> callElement.getTypeArguments()
                    .map { it.getTypeReference()?.let { TypeInfo(it, Variance.INVARIANT) } }
                    .filterNotNull()
        }

        val classInfo = ClassInfo(
                kind = if (isAnnotation) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS,
                name = calleeRef.getReferencedName(),
                targetParent = targetParent,
                expectedTypeInfo = TypeInfo.Empty,
                parameterInfos = parameterInfos,
                open = !isAnnotation,
                typeArguments = typeArgumentInfos
        )
        return CreateClassFromUsageFix(callElement, classInfo)
    }
}
