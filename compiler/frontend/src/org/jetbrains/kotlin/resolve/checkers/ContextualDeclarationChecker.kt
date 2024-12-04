/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isContextualDeclaration
import org.jetbrains.kotlin.resolve.checkContextReceiversAreEnabled
import org.jetbrains.kotlin.resolve.checkSubtypingBetweenContextReceivers

object ContextualDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            return
        }
        if (declaration.isContextualDeclaration()) {
            val contextReceiverList = declaration.findDescendantOfType<KtContextReceiverList>() ?: return
            checkContextReceiversAreEnabled(context.trace, context.languageVersionSettings, contextReceiverList)
            return
        }
    }
}

object SubtypingBetweenContextReceiversChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers) || !declaration.isContextualDeclaration()) {
            return
        }
        val contextReceivers = when (descriptor) {
            is CallableDescriptor -> descriptor.contextReceiverParameters
            is ClassDescriptor -> descriptor.contextReceivers
            else -> return
        }
        val contextReceiverList = declaration.findDescendantOfType<KtContextReceiverList>() ?: return
        checkSubtypingBetweenContextReceivers(context.trace, contextReceiverList, contextReceivers.map { it.type })
    }
}