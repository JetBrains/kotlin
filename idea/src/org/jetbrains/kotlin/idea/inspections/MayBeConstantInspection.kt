/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.AddConstModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.constants.evaluate.isStandaloneOnlyConstant
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MayBeConstantInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)

                if (property.isLocal || property.isVar) return
                val initializer = property.initializer ?: return
                if (property.hasModifier(KtTokens.CONST_KEYWORD)) return
                // Top-level or object only
                if (property.containingClassOrObject is KtClass) return

                // For some reason constant evaluation does not work for property.analyze()
                val context = initializer.analyze(BodyResolveMode.PARTIAL)
                val propertyDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? VariableDescriptor ?: return
                val withJvmField = propertyDescriptor.hasJvmFieldAnnotation()
                if (property.annotationEntries.isNotEmpty() && !withJvmField) return

                val initializerValue = ConstantExpressionEvaluator.getConstant(
                        initializer, context
                )?.toConstantValue(propertyDescriptor.type) ?: return
                if (initializerValue.isStandaloneOnlyConstant()) return

                holder.registerProblem(
                        property.nameIdentifier ?: property,
                        if (withJvmField) "'const' might be used instead of '@JvmField'" else "Might be 'const'",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        IntentionWrapper(AddConstModifierFix(property), property.containingFile)
                )
            }
        }
    }
}
