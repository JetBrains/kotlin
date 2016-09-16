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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiConstructorCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.MethodSignatureUtil
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.references.unwrappedTargets
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.utils.addToStdlib.check

val KtDeclaration.descriptor: DeclarationDescriptor?
    get() = this.analyze().get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)

val KtDeclaration.constructor: ConstructorDescriptor?
    get() {
        val context = this.analyze()
        return when (this) {
            is KtClassOrObject -> context[BindingContext.CLASS, this]?.unsubstitutedPrimaryConstructor
            is KtFunction -> context[BindingContext.CONSTRUCTOR, this]
            else -> null
        }
    }

val KtParameter.propertyDescriptor: PropertyDescriptor?
    get() = this.analyze().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)

fun PsiReference.checkUsageVsOriginalDescriptor(
        targetDescriptor: DeclarationDescriptor,
        declarationToDescriptor: (KtDeclaration) -> DeclarationDescriptor? = {it.descriptor},
        checker: (usageDescriptor: DeclarationDescriptor, targetDescriptor: DeclarationDescriptor) -> Boolean
): Boolean {
    return unwrappedTargets
            .filterIsInstance<KtDeclaration>()
            .any {
                val usageDescriptor = declarationToDescriptor(it)
                usageDescriptor != null && checker(usageDescriptor, targetDescriptor)
            }
}

fun PsiReference.isImportUsage(): Boolean =
        element!!.getNonStrictParentOfType<KtImportDirective>() != null

fun PsiReference.isConstructorUsage(ktClassOrObject: KtClassOrObject): Boolean = with (element!!) {
    fun checkJavaUsage(): Boolean {
        val call = getNonStrictParentOfType<PsiConstructorCall>()
        return call == parent && call?.resolveConstructor()?.containingClass?.navigationElement == ktClassOrObject
    }

    fun checkKotlinUsage(): Boolean {
        if (this !is KtElement) return false

        val descriptor = getConstructorCallDescriptor()
        if (descriptor !is ConstructorDescriptor) return false

        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.containingDeclaration)
        return declaration == ktClassOrObject || (declaration is KtConstructor<*> && declaration.getContainingClassOrObject() == ktClassOrObject)
    }

    checkJavaUsage() || checkKotlinUsage()
}

private fun KtElement.getConstructorCallDescriptor(): DeclarationDescriptor? {
    val bindingContext = this.analyze()
    val constructorCalleeExpression = getNonStrictParentOfType<KtConstructorCalleeExpression>()
    if (constructorCalleeExpression != null) {
        return bindingContext.get(BindingContext.REFERENCE_TARGET, constructorCalleeExpression.constructorReferenceExpression)
    }

    val callExpression = getNonStrictParentOfType<KtCallElement>()
    if (callExpression != null) {
        val callee = callExpression.calleeExpression
        if (callee is KtReferenceExpression) {
            return bindingContext.get(BindingContext.REFERENCE_TARGET, callee)
        }
    }

    return null
}

fun PsiElement.processDelegationCallConstructorUsages(scope: SearchScope, process: (KtCallElement) -> Boolean): Boolean {
    val task = buildProcessDelegationCallConstructorUsagesTask(scope, process)
    return task()
}

// should be executed under read-action, returns long-running part to be executed outside read-action
fun PsiElement.buildProcessDelegationCallConstructorUsagesTask(scope: SearchScope, process: (KtCallElement) -> Boolean): () -> Boolean {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    val task1 = buildProcessDelegationCallKotlinConstructorUsagesTask(scope, process)
    val task2 = buildProcessDelegationCallJavaConstructorUsagesTask(scope, process)
    return { task1() && task2() }
}

private fun PsiElement.buildProcessDelegationCallKotlinConstructorUsagesTask(scope: SearchScope, process: (KtCallElement) -> Boolean): () -> Boolean {
    val element = unwrapped
    if (element != null && element !in scope) return { true }

    val klass = when (element) {
        is KtConstructor<*> -> element.getContainingClassOrObject()
        is KtClass -> element
        else -> return { true }
    }

    if (klass !is KtClass || element !is KtDeclaration) return { true }
    val descriptor = element.constructor ?: return { true }

    if (!processClassDelegationCallsToSpecifiedConstructor(klass, descriptor, process)) return { false }

    // long-running task, return it to execute outside read-action
    return { processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, descriptor, process) }
}

private fun PsiElement.buildProcessDelegationCallJavaConstructorUsagesTask(scope: SearchScope, process: (KtCallElement) -> Boolean): () -> Boolean {
    if (this is KtLightElement<*, *>) return { true }
    // TODO: Temporary hack to avoid NPE while KotlinNoOriginLightMethod is around
    if (this is KtLightMethod && this.kotlinOrigin == null) return { true }
    if (!(this is PsiMethod && isConstructor)) return { true }
    val klass = containingClass ?: return { true }
    val descriptor = getJavaMethodDescriptor() as? ConstructorDescriptor ?: return { true }
    return { processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, descriptor, process) }
}


private fun processInheritorsDelegatingCallToSpecifiedConstructor(
        klass: PsiElement,
        scope: SearchScope,
        descriptor: ConstructorDescriptor,
        process: (KtCallElement) -> Boolean
): Boolean {
    return HierarchySearchRequest(klass, scope, false).searchInheritors().all {
        runReadAction {
            val unwrapped = it.check { it.isValid }?.unwrapped
            if (unwrapped is KtClass)
                processClassDelegationCallsToSpecifiedConstructor(unwrapped, descriptor, process)
            else
                true
        }
    }
}

private fun processClassDelegationCallsToSpecifiedConstructor(
        klass: KtClass, constructor: DeclarationDescriptor, process: (KtCallElement) -> Boolean
): Boolean {
    for (secondaryConstructor in klass.getSecondaryConstructors()) {
        val delegationCallDescriptor = secondaryConstructor.getDelegationCall().getConstructorCallDescriptor()
        if (constructor == delegationCallDescriptor) {
            if (!process(secondaryConstructor.getDelegationCall())) return false
        }
    }
    if (!klass.isEnum()) return true
    for (declaration in klass.declarations) {
        if (declaration is KtEnumEntry) {
            val delegationCall = declaration.getSuperTypeListEntries().firstOrNull()
            if (delegationCall is KtSuperTypeCallEntry && constructor == delegationCall.calleeExpression.getConstructorCallDescriptor()) {
                if (!process(delegationCall)) return false
            }
        }
    }
    return true
}

// Check if reference resolves to extension function whose receiver is the same as declaration's parent (or its superclass)
// Used in extension search
fun PsiReference.isExtensionOfDeclarationClassUsage(declaration: KtNamedDeclaration): Boolean {
    val descriptor = declaration.descriptor ?: return false
    return checkUsageVsOriginalDescriptor(descriptor) { usageDescriptor, targetDescriptor ->
        when {
            usageDescriptor == targetDescriptor -> false
            usageDescriptor !is FunctionDescriptor -> false
            else -> {
                val receiverDescriptor =
                        usageDescriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor
                val containingDescriptor = targetDescriptor.containingDeclaration

                containingDescriptor == receiverDescriptor
                || (containingDescriptor is ClassDescriptor
                    && receiverDescriptor is ClassDescriptor
                    && DescriptorUtils.isSubclass(containingDescriptor, receiverDescriptor))
            }
        }
    }
}

// Check if reference resolves to the declaration with the same parent
// Used in overload search
fun PsiReference.isUsageInContainingDeclaration(declaration: KtNamedDeclaration): Boolean {
    val descriptor = declaration.descriptor ?: return false
    return checkUsageVsOriginalDescriptor(descriptor) { usageDescriptor, targetDescriptor ->
        usageDescriptor != targetDescriptor
        && usageDescriptor.containingDeclaration == targetDescriptor.containingDeclaration
    }
}

fun PsiReference.isCallableOverrideUsage(declaration: KtNamedDeclaration): Boolean {
    val toDescriptor: (KtDeclaration) -> CallableDescriptor? = { declaration ->
        if (declaration is KtParameter) {
            // we don't treat parameters in overriding method as "override" here (overriding parameters usages are searched optionally and via searching of overriding methods first)
            if (declaration.hasValOrVar()) declaration.propertyDescriptor else null
        }
        else {
            declaration.descriptor as? CallableDescriptor
        }
    }

    val targetDescriptor = toDescriptor(declaration) ?: return false

    return unwrappedTargets.any {
        when (it) {
            is KtDeclaration -> {
                val usageDescriptor = toDescriptor(it)
                usageDescriptor != null && OverridingUtil.overrides(usageDescriptor, targetDescriptor)
            }
            is PsiMethod -> {
                declaration.toLightMethods().any { superMethod -> MethodSignatureUtil.isSuperMethod(superMethod, it) }
            }
            else -> false
        }
    }
}
