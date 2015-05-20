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
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.JetTypeReference
import java.util.Collections
import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.JetReferenceExpression
import java.util.Arrays
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.isDotReceiver
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

public object CreateClassFromReferenceExpressionActionFactory : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        fun isEnum(element: PsiElement): Boolean {
            return when (element) {
                is JetClass -> element.isEnum()
                is PsiClass -> element.isEnum()
                else -> false
            }
        }

        val refExpr = diagnostic.getPsiElement() as? JetSimpleNameExpression ?: return Collections.emptyList()
        if (refExpr.getNonStrictParentOfType<JetTypeReference>() != null) return Collections.emptyList()

        val file = refExpr.getContainingFile() as? JetFile ?: return Collections.emptyList()

        val name = refExpr.getReferencedName()

        val (context, moduleDescriptor) = refExpr.analyzeFullyAndGetResult()

        val fullCallExpr = refExpr.getParent()?.let {
            when {
                it is JetCallExpression && it.getCalleeExpression() == refExpr -> return Collections.emptyList()
                it is JetQualifiedExpression && it.getSelectorExpression() == refExpr -> it
                else -> refExpr
            }
        } as? JetExpression ?: return Collections.emptyList()

        val inImport = refExpr.getNonStrictParentOfType<JetImportDirective>() != null
        val qualifierExpected = refExpr.isDotReceiver() || ((refExpr.getParent() as? JetDotQualifiedExpression)?.isDotReceiver() ?: false)

        if (inImport || qualifierExpected) {
            val receiverSelector = (fullCallExpr as? JetQualifiedExpression)?.getReceiverExpression()?.getQualifiedElementSelector() as? JetReferenceExpression
            val qualifierDescriptor = receiverSelector?.let { context[BindingContext.REFERENCE_TARGET, it] }

            val targetParent =
                    getTargetParentByQualifier(refExpr.getContainingJetFile(), receiverSelector != null, qualifierDescriptor)
                    ?: return Collections.emptyList()

            val createPackageAction = refExpr.getCreatePackageFixIfApplicable(targetParent)
            if (createPackageAction != null) return Collections.singletonList(createPackageAction)

            return (if (name.checkClassName()) ClassKind.values() else arrayOf())
                    .filter {
                        when (it) {
                            ClassKind.ANNOTATION_CLASS -> inImport
                            ClassKind.ENUM_ENTRY -> inImport && isEnum(targetParent)
                            else -> true
                        }
                    }
                    .map {
                        val classInfo = ClassInfo(
                                kind = it,
                                name = name,
                                targetParent = targetParent,
                                expectedTypeInfo = TypeInfo.Empty
                        )
                        CreateClassFromUsageFix(refExpr, classInfo)
                    }
        }

        if (fullCallExpr.getAssignmentByLHS() != null) return Collections.emptyList()

        val call = refExpr.getCall(context) ?: return Collections.emptyList()
        val targetParent = getTargetParentByCall(call, file) ?: return Collections.emptyList()
        if (isInnerClassExpected(call)) return Collections.emptyList()

        val (expectedTypeInfo, filter) = fullCallExpr.getInheritableTypeInfo(context, moduleDescriptor, targetParent)

        return Arrays.asList(ClassKind.OBJECT, ClassKind.ENUM_ENTRY)
                .filter {
                    filter(it) && when (it) {
                        ClassKind.OBJECT -> true
                        ClassKind.ENUM_ENTRY -> isEnum(targetParent)
                        else -> false
                    }
                }
                .map {
                    val classInfo = ClassInfo(
                            kind = it,
                            name = name,
                            targetParent = targetParent,
                            expectedTypeInfo = expectedTypeInfo
                    )
                    CreateClassFromUsageFix(refExpr, classInfo)
                }
    }
}
