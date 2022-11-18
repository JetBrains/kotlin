/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.JvmNames.JVM_DELEGATE_TO_DEFAULTS_FQ_NAME
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils.getAllOverriddenDescriptors
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object DelegatedDefaultInterfaceMethodsAreOverriddenChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.allowImplicitDelegationToDefaults)) return
        if (descriptor !is ClassDescriptor) return
        if (declaration !is KtClassOrObject) return

        val suspiciousDelegatedTypes = declaration.superTypeListEntries
            .asSequence()
            .filterIsInstance<KtDelegatedSuperTypeEntry>()
            .filter { !delegateToDefaults(it, context) }
            .mapNotNull { it.typeReference }
            .mapNotNull { context.trace.get(BindingContext.TYPE, it) }
            .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
            .toSet()

        if (suspiciousDelegatedTypes.isEmpty()) return

        descriptor.defaultType.memberScope.getContributedDescriptors()
            .asSequence()
            .filterIsInstance<FunctionDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            .flatMap { getAllOverriddenDescriptors(it) }
            .firstOrNull { it.isDefaultJavaMethod && it.containingDeclaration in suspiciousDelegatedTypes }
            ?.let {
                context.trace.report(ErrorsJvm.NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD.on(declaration, it.name.asString()))
                return
            }
    }

    private fun delegateToDefaults(entry: KtDelegatedSuperTypeEntry, context: DeclarationCheckerContext): Boolean {
        val annotated = (deparenthesize(entry.delegateExpression, true) as? KtAnnotatedExpression) ?: return false
        return annotated.annotationEntries.any { context.trace[BindingContext.ANNOTATION, it]?.fqName == JVM_DELEGATE_TO_DEFAULTS_FQ_NAME }
    }

    private val CallableMemberDescriptor.isDefaultJavaMethod: Boolean
        get() = kind == CallableMemberDescriptor.Kind.DECLARATION && (containingDeclaration as? JavaClassDescriptor)?.kind == ClassKind.INTERFACE
}
