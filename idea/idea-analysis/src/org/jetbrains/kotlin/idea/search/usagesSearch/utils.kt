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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiConstructorCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.asJava.KotlinLightElement
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.idea.findUsages.UsageTypeUtils
import org.jetbrains.kotlin.idea.findUsages.UsageTypeEnum
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors

val JetDeclaration.descriptor: DeclarationDescriptor?
    get() = this.analyze().get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)

val JetDeclaration.constructor: ConstructorDescriptor?
    get() {
        val context = this.analyze()
        return when (this) {
            is JetClassOrObject -> context[BindingContext.CLASS, this]?.getUnsubstitutedPrimaryConstructor()
            is JetFunction -> context[BindingContext.CONSTRUCTOR, this]
            else -> null
        }
    }

val JetParameter.propertyDescriptor: PropertyDescriptor?
    get() = this.analyze().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)

fun PsiReference.checkUsageVsOriginalDescriptor(
        target: JetDeclaration,
        declarationToDescriptor: (JetDeclaration) -> DeclarationDescriptor? = {it.descriptor},
        checker: (usageDescriptor: DeclarationDescriptor, targetDescriptor: DeclarationDescriptor) -> Boolean
): Boolean {
    return unwrappedTargets.any {
        if (it is JetDeclaration) {
            val usageDescriptor = declarationToDescriptor(it)
            val targetDescriptor = declarationToDescriptor(target)
            usageDescriptor != null && targetDescriptor != null && checker(usageDescriptor, targetDescriptor)
        }
        else false
    }
}

fun PsiReference.isImportUsage(): Boolean =
        getElement()!!.getNonStrictParentOfType<JetImportDirective>() != null

fun PsiReference.isConstructorUsage(jetClassOrObject: JetClassOrObject): Boolean = with (getElement()!!) {
    fun checkJavaUsage(): Boolean {
        val call = getNonStrictParentOfType<PsiConstructorCall>()
        return call == getParent() && call?.resolveConstructor()?.getContainingClass()?.getNavigationElement() == jetClassOrObject
    }

    fun checkKotlinUsage(): Boolean {
        if (this !is JetElement) return false

        val descriptor = getConstructorCallDescriptor()
        if (descriptor !is ConstructorDescriptor) return false

        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.getContainingDeclaration())
        return declaration == jetClassOrObject || (declaration is JetConstructor<*> && declaration.getClassOrObject() == jetClassOrObject)
    }

    checkJavaUsage() || checkKotlinUsage()
}

private fun JetElement.getConstructorCallDescriptor(): DeclarationDescriptor? {
    val bindingContext = this.analyze()
    val constructorCalleeExpression = getNonStrictParentOfType<JetConstructorCalleeExpression>()
    if (constructorCalleeExpression != null) {
        return bindingContext.get(BindingContext.REFERENCE_TARGET, constructorCalleeExpression.getConstructorReferenceExpression())
    }

    val callExpression = getNonStrictParentOfType<JetCallElement>()
    if (callExpression != null) {
        val callee = callExpression.getCalleeExpression()
        if (callee is JetReferenceExpression) {
            return bindingContext.get(BindingContext.REFERENCE_TARGET, callee)
        }
    }

    return null
}

public fun PsiElement.processDelegationCallConstructorUsages(scope: SearchScope, process: (JetConstructorDelegationCall) -> Unit) {
    processDelegationCallKotlinConstructorUsages(scope, process)
    processDelegationCallJavaConstructorUsages(scope, process)
}

private fun PsiElement.processDelegationCallKotlinConstructorUsages(scope: SearchScope, process: (JetConstructorDelegationCall) -> Unit) {
    val element = unwrapped
    val klass = when (element) {
        is JetConstructor<*> -> element.getClassOrObject()
        is JetClass -> element
        else -> return
    }

    if (klass !is JetClass || element !is JetDeclaration) return
    val descriptor = element.constructor ?: return

    processClassDelegationCallsToSpecifiedConstructor(klass, descriptor, process)
    processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, descriptor, process)
}

private fun PsiElement.processDelegationCallJavaConstructorUsages(scope: SearchScope, process: (JetConstructorDelegationCall) -> Unit) {
    if (this is KotlinLightElement<*, *>) return
    if (!(this is PsiMethod && isConstructor())) return
    val klass = getContainingClass() ?: return
    val descriptor = getJavaMethodDescriptor() as? ConstructorDescriptor ?: return
    processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, descriptor, process)
}


private fun processInheritorsDelegatingCallToSpecifiedConstructor(
        klass: PsiElement,
        scope: SearchScope,
        descriptor: ConstructorDescriptor,
        process: (JetConstructorDelegationCall) -> Unit
) {
    HierarchySearchRequest(klass, scope, false).searchInheritors().forEach() {
        val unwrapped = it.unwrapped
        if (unwrapped is JetClass) {
            processClassDelegationCallsToSpecifiedConstructor(unwrapped, descriptor, process)
        }
    }
}

private fun processClassDelegationCallsToSpecifiedConstructor(
        klass: JetClass, constructor: DeclarationDescriptor, process: (JetConstructorDelegationCall) -> Unit
) {
    for (secondaryConstructor in klass.getSecondaryConstructors()) {
        val delegationCallDescriptor = secondaryConstructor.getDelegationCall().getConstructorCallDescriptor()
        if (constructor == delegationCallDescriptor) {
            process(secondaryConstructor.getDelegationCall())
        }
    }
}

// Check if reference resolves to extension function whose receiver is the same as declaration's parent (or its superclass)
// Used in extension search
fun PsiReference.isExtensionOfDeclarationClassUsage(declaration: JetNamedDeclaration): Boolean =
        checkUsageVsOriginalDescriptor(declaration) { usageDescriptor, targetDescriptor ->
            when {
                usageDescriptor == targetDescriptor -> false
                usageDescriptor !is FunctionDescriptor -> false
                else -> {
                    val receiverDescriptor =
                            usageDescriptor.getExtensionReceiverParameter()?.getType()?.getConstructor()?.getDeclarationDescriptor()
                    val containingDescriptor = targetDescriptor.getContainingDeclaration()

                    containingDescriptor == receiverDescriptor
                    || (containingDescriptor is ClassDescriptor
                        && receiverDescriptor is ClassDescriptor
                        && DescriptorUtils.isSubclass(containingDescriptor, receiverDescriptor))
                }
            }
        }

// Check if reference resolves to the declaration with the same parent
// Used in overload search
fun PsiReference.isUsageInContainingDeclaration(declaration: JetNamedDeclaration): Boolean =
        checkUsageVsOriginalDescriptor(declaration) { usageDescriptor, targetDescriptor ->
            usageDescriptor != targetDescriptor
            && usageDescriptor.getContainingDeclaration() == targetDescriptor.getContainingDeclaration()
        }

fun PsiReference.isCallableOverrideUsage(declaration: JetNamedDeclaration): Boolean {
    val decl2Desc = { declaration: JetDeclaration ->
        if (declaration is JetParameter && declaration.hasValOrVar()) declaration.propertyDescriptor else declaration.descriptor
    }

    return checkUsageVsOriginalDescriptor(declaration, decl2Desc) { usageDescriptor, targetDescriptor ->
        usageDescriptor is CallableDescriptor && targetDescriptor is CallableDescriptor
        && OverrideResolver.overrides(usageDescriptor, targetDescriptor)
    }
}


// Check if reference resolves to property getter
// Works for JetProperty and JetParameter
fun PsiReference.isPropertyReadOnlyUsage(): Boolean {
    if (UsageTypeUtils.getUsageType(getElement()) == UsageTypeEnum.READ) return true

    val refTarget = resolve()
    if (refTarget is KotlinLightMethod) {
        val origin = refTarget.getOrigin()
        val declaration: JetNamedDeclaration? = when (origin) {
            is JetPropertyAccessor -> origin.getNonStrictParentOfType<JetProperty>()
            is JetProperty, is JetParameter -> origin as JetNamedDeclaration
            else -> null
        }
        return declaration != null && refTarget.getName() == PropertyCodegen.getterName(declaration.getNameAsName())
    }

    return false
}
