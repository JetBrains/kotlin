/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

object EnumCompanionInEnumConstructorCallChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtEnumEntry || descriptor !is ClassDescriptor) return
        if (descriptor.kind != ClassKind.ENUM_ENTRY) return
        val enumDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return
        val enumCompanion = enumDescriptor.companionObjectDescriptor ?: return
        val initializer = declaration.initializerList?.initializers?.firstIsInstanceOrNull<KtSuperTypeCallEntry>() ?: return
        val bindingTrace = context.trace
        val visitor = Visitor(
            enumDescriptor,
            enumCompanion,
            bindingTrace.bindingContext,
            bindingTrace,
            reportError = context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitAccessToEnumCompanionMembersInEnumConstructorCall)
        )
        initializer.acceptChildren(visitor)
    }

    private class Visitor(
        val enumDescriptor: ClassDescriptor,
        val companionDescriptor: ClassDescriptor,
        val context: BindingContext,
        val reporter: DiagnosticSink,
        val reportError: Boolean
    ) : KtVisitorVoid() {
        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitExpression(expression: KtExpression) {
            val needAnalyzeReceiver = analyzeExpression(expression)
            if (needAnalyzeReceiver) {
                expression.acceptChildren(this)
            } else if (expression is KtCallExpression) {
                expression.valueArgumentList?.acceptChildren(this)
            }
        }

        private fun analyzeExpression(expression: KtExpression): Boolean {
            if (expression.parent is KtCallExpression) return true
            val resolvedCall = expression.getResolvedCall(context) ?: return true

            val dispatchDescriptor = resolvedCall.dispatchReceiver.resolvedDescriptor
            val extensionDescriptor = resolvedCall.extensionReceiver.resolvedDescriptor
            val dispatchIsCompanion = dispatchDescriptor == companionDescriptor
            val extensionIsCompanion = extensionDescriptor == companionDescriptor

            if (dispatchIsCompanion || extensionIsCompanion) {
                val reportOn = when (val receiverExpression = (expression as? KtQualifiedExpression)?.receiverExpression) {
                    is KtSimpleNameExpression -> receiverExpression
                    is KtQualifiedExpression -> receiverExpression.selectorExpression
                    else -> null
                } ?: expression
                val factory = if (reportError) {
                    Errors.UNINITIALIZED_ENUM_COMPANION
                } else {
                    Errors.UNINITIALIZED_ENUM_COMPANION_WARNING
                }
                reporter.report(factory.on(reportOn, enumDescriptor))
                return false
            }
            return true
        }
    }

    private val ReceiverValue?.resolvedDescriptor: DeclarationDescriptor?
        get() {
            if (this !is ClassValueReceiver && this !is ImplicitClassReceiver) return null
            return this.type.unwrap().constructor.declarationDescriptor
        }
}
