/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.resolve.source.getPsi

class SuspendInFunInterfaceChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtClass) return
        if (descriptor !is ClassDescriptor || !descriptor.isFun) return

        val funKeyword = declaration.getFunKeyword() ?: return

        val abstractMember = getSingleAbstractMethodOrNull(descriptor) ?: return
        if (!abstractMember.isSuspend) return

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionsInFunInterfaces) &&
            context.languageVersionSettings.getFlag(JvmAnalysisFlags.useIR)
        ) return

        val ktFunction = abstractMember.source.getPsi() as? KtNamedFunction
        val reportOn = ktFunction?.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) ?: funKeyword
        context.trace.report(Errors.FUN_INTERFACE_WITH_SUSPEND_FUNCTION.on(reportOn))
    }
}