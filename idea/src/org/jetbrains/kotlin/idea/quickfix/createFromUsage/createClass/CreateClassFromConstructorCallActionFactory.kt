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
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import java.util.Collections
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*

public object CreateClassFromConstructorCallActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val diagElement = diagnostic.getPsiElement()
        if (diagElement.getNonStrictParentOfType<JetTypeReference>() != null) return null

        val inAnnotationEntry = diagElement.getNonStrictParentOfType<JetAnnotationEntry>() != null

        val callExpr = diagElement.getParent() as? JetCallExpression ?: return null
        if (callExpr.getCalleeExpression() != diagElement) return null

        val calleeExpr = callExpr.getCalleeExpression() as? JetSimpleNameExpression ?: return null

        val name = calleeExpr.getReferencedName()
        if (!inAnnotationEntry && !name.checkClassName()) return null

        val callParent = callExpr.getParent()
        val fullCallExpr =
                if (callParent is JetQualifiedExpression && callParent.getSelectorExpression() == callExpr) callParent else callExpr

        val file = fullCallExpr.getContainingFile() as? JetFile ?: return null

        val (context, moduleDescriptor) = callExpr.analyzeFullyAndGetResult()

        val call = callExpr.getCall(context) ?: return null
        val targetParent = getTargetParentByCall(call, file) ?: return null
        val inner = isInnerClassExpected(call)

        val valueArguments = callExpr.getValueArguments()
        val defaultParamName = if (inAnnotationEntry && valueArguments.size == 1) "value" else null
        val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()
        val parameterInfos = valueArguments.map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.getReferenceExpression()?.getReferencedName() ?: defaultParamName
            )
        }

        val classKind = if (inAnnotationEntry) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS

        val (expectedTypeInfo, filter) = fullCallExpr.getInheritableTypeInfo(context, moduleDescriptor, targetParent)
        if (!filter(classKind)) return null

        val typeArgumentInfos = if (inAnnotationEntry) Collections.emptyList() else callExpr.getTypeInfoForTypeArguments()

        val classInfo = ClassInfo(
                kind = classKind,
                name = name,
                targetParent = targetParent,
                expectedTypeInfo = expectedTypeInfo,
                inner = inner,
                typeArguments = typeArgumentInfos,
                parameterInfos = parameterInfos
        )
        return CreateClassFromUsageFix(callExpr, classInfo)
    }
}
