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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.testLog
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.util.isValidOperator
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

//TODO: problem with infinite recursion not solved

//TODO: code duplication

fun findInvokeOperatorUsages(
        invokeFunction: PsiMethod,
        scope: SearchScope,
        consumer: Processor<PsiReference>
) {
    if (invokeFunction !is KtLightMethod) return //TODO
    val ktDeclarationTarget = invokeFunction.kotlinOrigin as? KtDeclaration ?: return //TODO?
    findInvokeOperatorUsages(ktDeclarationTarget, scope, consumer)
}

fun findInvokeOperatorUsages(
        targetDeclaration: KtDeclaration,
        searchScope: SearchScope,
        consumer: Processor<PsiReference>
) {
    val usePlainSearch = when (ExpressionsOfTypeProcessor.mode) {
        ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART -> false
        ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN -> true
        ExpressionsOfTypeProcessor.Mode.PLAIN_WHEN_NEEDED -> searchScope is LocalSearchScope // for local scope it's faster to use plain search
    }
    if (usePlainSearch) {
        doPlainSearch(targetDeclaration, searchScope, consumer)
        return
    }

    val descriptor = targetDeclaration.resolveToDescriptor() as? CallableDescriptor ?: return

    if (descriptor is FunctionDescriptor && !descriptor.isValidOperator()) return

    val dataType = if (descriptor.isExtension) {
        descriptor.fuzzyExtensionReceiverType()!!
    }
    else {
        val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return
        classDescriptor.defaultType.toFuzzyType(classDescriptor.typeConstructor.parameters)
    }

    ExpressionsOfTypeProcessor(
            dataType,
            searchScope,
            suspiciousExpressionHandler = { expression -> processSuspiciousExpression(expression, targetDeclaration, consumer) },
            suspiciousScopeHandler = { searchScope -> doPlainSearch(targetDeclaration, searchScope, consumer) },
            resolutionFacade = targetDeclaration.getResolutionFacade()
    ).run()
}

private fun processSuspiciousExpression(expression: KtExpression, targetDeclaration: KtDeclaration, consumer: Processor<PsiReference>) {
    val callExpression = expression.parent as? KtCallExpression ?: return
    testLog?.add("Resolving call ${callExpression.text}")
    processCallExpression(callExpression, targetDeclaration, consumer)
}

private fun doPlainSearch(ktDeclaration: KtDeclaration, scope: SearchScope, consumer: Processor<PsiReference>) {
    if (scope is LocalSearchScope) {
        for (element in scope.scope) {
            val stop = element.anyDescendantOfType<KtCallExpression> { !processCallExpression(it, ktDeclaration, consumer) }
            if (stop) break
        }
    }
    else {
        scope as GlobalSearchScope
        val project = ktDeclaration.project
        val psiManager = PsiManager.getInstance(project)
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
            if (file in scope) {
                val ktFile = psiManager.findFile(file) as? KtFile
                if (ktFile != null) {
                    val stop = ktFile.anyDescendantOfType<KtCallExpression> {
                        !processCallExpression(it, ktDeclaration, consumer)
                    }
                    if (stop) return@iterateContent false
                }
            }
            true
        }
    }
}

private fun processCallExpression(callExpression: KtCallExpression, targetDeclaration: KtDeclaration, consumer: Processor<PsiReference>): Boolean {
    val reference = callExpression.references.firstIsInstance<KtInvokeFunctionReference>()
    if (reference.isReferenceTo(targetDeclaration)) {
        return consumer.process(reference)
    }
    else {
        return true
    }
}
