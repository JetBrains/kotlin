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

import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory
import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.idea.quickfix.QuickFixUtil
import java.util.Collections
import org.jetbrains.kotlin.psi.JetUserType
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.psi.JetDelegatorToSuperClass
import org.jetbrains.kotlin.psi.JetConstructorCalleeExpression
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.builtins.*

public object CreateClassFromTypeReferenceActionFactory: JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val userType = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetUserType>()) ?: return Collections.emptyList()
        val typeArguments = userType.getTypeArgumentsAsTypes()

        val refExpr = userType.getReferenceExpression() ?: return Collections.emptyList()
        val name = refExpr.getReferencedName()

        val typeRefParent = userType.getParent()?.getParent()
        if (typeRefParent is JetConstructorCalleeExpression) return Collections.emptyList()

        val traitExpected = typeRefParent is JetDelegatorToSuperClass

        val context = userType.analyze()

        val file = userType.getContainingFile() as? JetFile ?: return Collections.emptyList()

        val isQualifier = (userType.getParent() as? JetUserType)?.let { it.getQualifier() == userType } ?: false
        val qualifier = userType.getQualifier()?.getReferenceExpression()
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParent = getTargetParentByQualifier(file, qualifier != null, qualifierDescriptor) ?: return Collections.emptyList()

        val possibleKinds = when {
            traitExpected -> Collections.singletonList(ClassKind.TRAIT)
            else -> ClassKind.values().filter {
                val noTypeArguments = typeArguments.isEmpty()
                when (it) {
                    ClassKind.OBJECT -> noTypeArguments && isQualifier
                    ClassKind.ANNOTATION_CLASS -> noTypeArguments && !isQualifier
                    ClassKind.ENUM_ENTRY -> false
                    ClassKind.ENUM_CLASS -> noTypeArguments
                    else -> true
                }
            }
        }

        val anyType = KotlinBuiltIns.getInstance().getAnyType()

        val createPackageAction = refExpr.getCreatePackageFixIfApplicable(targetParent)
        val createClassActions = possibleKinds.map {
            val classInfo = ClassInfo(
                    kind = it,
                    name = name,
                    targetParent = targetParent,
                    expectedTypeInfo = TypeInfo.Empty,
                    typeArguments = typeArguments.map {
                        if (it != null) TypeInfo(it, Variance.INVARIANT) else TypeInfo(anyType, Variance.INVARIANT)
                    }
            )
            CreateClassFromUsageFix(userType, classInfo)
        }
        return createPackageAction.singletonOrEmptyList() + createClassActions
    }
}
