/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DeprecationResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils

interface ClassifierUsageChecker {
    fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext)
}

class ClassifierUsageCheckerContext(
    override val trace: BindingTrace,
    override val languageVersionSettings: LanguageVersionSettings,
    override val deprecationResolver: DeprecationResolver,
    override val moduleDescriptor: ModuleDescriptor
) : CheckerContext


fun checkClassifierUsages(
    declarations: Collection<PsiElement>,
    checkers: Iterable<ClassifierUsageChecker>,
    context: ClassifierUsageCheckerContext
) {
    val visitor = object : KtTreeVisitorVoid() {
        override fun visitReferenceExpression(expression: KtReferenceExpression) {
            super.visitReferenceExpression(expression)

            if (expression is KtNameReferenceExpression && expression.getReferencedNameElementType() == KtTokens.SUPER_KEYWORD) {
                // Do not run checkers here because super expressions is not a proper expression. For example,
                // it doesn't make sense to report deprecation on "super" in "super.foo()" if the super class is deprecated.
                return
            }

            val target = getReferencedClassifier(expression) ?: return

            runCheckersWithTarget(target, expression)

            getReferenceToCompanionViaClassifier(expression, target)?.let { referenceClassifier ->
                val outerClass = target.containingDeclaration as ClassDescriptor
                runCheckersWithTarget(outerClass, expression)
                if (referenceClassifier is TypeAliasDescriptor) {
                    runCheckersWithTarget(referenceClassifier, expression)
                }
            }
        }

        private fun runCheckersWithTarget(target: ClassifierDescriptor, expression: KtReferenceExpression) {
            for (checker in checkers) {
                checker.check(target, expression, context)
            }
        }

        private fun getReferencedClassifier(expression: KtReferenceExpression): ClassifierDescriptor? {
            val target = context.trace.get(BindingContext.REFERENCE_TARGET, expression)
            if (target is ClassifierDescriptor) return target
            if (target is ClassConstructorDescriptor) return target.constructedClass

            // "Comparable" in "import java.lang.Comparable" references both a class and a SAM constructor and prevents
            // REFERENCE_TARGET from being recorded in favor of AMBIGUOUS_REFERENCE_TARGET. But we must still run checkers
            // to report if there's something wrong with the class. We characterize this case below by the following properties:
            // 1) Exactly one of the references is a classifier
            // 2) All references refer to the same source element, i.e. their source is the same
            val targets = context.trace.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, expression) ?: return null
            if (targets.groupBy { (it as? DeclarationDescriptorWithSource)?.source }.size != 1) return null
            return targets.filterIsInstance<ClassifierDescriptor>().singleOrNull()
        }

        private fun getReferenceToCompanionViaClassifier(
            expression: KtReferenceExpression,
            target: ClassifierDescriptor
        ): ClassifierDescriptor? {
            if (!DescriptorUtils.isCompanionObject(target)) return null
            return context.trace.get(BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, expression)
        }
    }

    for (declaration in declarations) {
        declaration.accept(visitor)
    }
}
