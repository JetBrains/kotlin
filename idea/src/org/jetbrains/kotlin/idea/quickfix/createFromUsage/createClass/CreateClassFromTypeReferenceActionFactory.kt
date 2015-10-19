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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtDelegatorToSuperClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.Variance
import java.util.Collections

public object CreateClassFromTypeReferenceActionFactory : CreateClassFromUsageFactory<KtUserType>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtUserType? {
        return QuickFixUtil.getParentElementOfType(diagnostic, javaClass<KtUserType>())
    }

    override fun getPossibleClassKinds(element: KtUserType, diagnostic: Diagnostic): List<ClassKind> {
        val typeRefParent = element.parent?.parent
        if (typeRefParent is KtConstructorCalleeExpression) return Collections.emptyList()

        val interfaceExpected = typeRefParent is KtDelegatorToSuperClass

        val isQualifier = (element.parent as? KtUserType)?.let { it.qualifier == element } ?: false

        return when {
            interfaceExpected -> Collections.singletonList(ClassKind.INTERFACE)
            else -> ClassKind.values().filter {
                val noTypeArguments = element.typeArgumentsAsTypes.isEmpty()
                when (it) {
                    ClassKind.OBJECT -> noTypeArguments && isQualifier
                    ClassKind.ANNOTATION_CLASS -> noTypeArguments && !isQualifier
                    ClassKind.ENUM_ENTRY -> false
                    ClassKind.ENUM_CLASS -> noTypeArguments
                    else -> true
                }
            }
        }
    }

    override fun extractFixData(element: KtUserType, diagnostic: Diagnostic): ClassInfo? {
        val name = element.referenceExpression?.getReferencedName() ?: return null
        if (element.parent?.parent is KtConstructorCalleeExpression) return null

        val file = element.containingFile as? KtFile ?: return null

        val (context, module) = element.analyzeAndGetResult()
        val qualifier = element.qualifier?.referenceExpression
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParent = getTargetParentByQualifier(file, qualifier != null, qualifierDescriptor) ?: return null

        val anyType = module.builtIns.anyType

        return ClassInfo(
                name = name,
                targetParent = targetParent,
                expectedTypeInfo = TypeInfo.Empty,
                typeArguments = element.typeArgumentsAsTypes.map {
                    if (it != null) TypeInfo(it, Variance.INVARIANT) else TypeInfo(anyType, Variance.INVARIANT)
                }
        )
    }
}
