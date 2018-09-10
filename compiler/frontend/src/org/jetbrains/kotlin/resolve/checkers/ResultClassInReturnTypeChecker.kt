/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.types.KotlinType

class ResultClassInReturnTypeChecker : DeclarationChecker {
    companion object {
        private const val RESULT_NAME = "Result"
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtCallableDeclaration || descriptor !is CallableMemberDescriptor) return

        val returnType = descriptor.returnType ?: return
        if (isForbiddenReturnType(returnType, descriptor)) {
            val typeReferenceOrDeclarationName = declaration.typeReference ?: declaration.nameIdentifier ?: return
            context.trace.reportDiagnosticOnce(Errors.RESULT_CLASS_IN_RETURN_TYPE.on(typeReferenceOrDeclarationName))
        }
    }

    private fun isForbiddenReturnType(returnType: KotlinType, declarationDescriptor: DeclarationDescriptor): Boolean {
        val descriptor = returnType.constructor.declarationDescriptor ?: return false
        if (!descriptor.isResultClass()) return false

        if (declarationDescriptor is PropertyDescriptor || declarationDescriptor is PropertyGetterDescriptor) {
            val visibility = (declarationDescriptor as DeclarationDescriptorWithVisibility).effectiveVisibility()
            return when (visibility) {
                is EffectiveVisibility.Private, is EffectiveVisibility.Local,
                is EffectiveVisibility.InternalOrPackage, is EffectiveVisibility.InternalProtected,
                is EffectiveVisibility.InternalProtectedBound -> false

                is EffectiveVisibility.Public, is EffectiveVisibility.Protected,
                is EffectiveVisibility.ProtectedBound -> true
            }
        }

        return true
    }

    private fun DeclarationDescriptor.isResultClass(): Boolean {
        val container = containingDeclaration ?: return false
        return container is PackageFragmentDescriptor &&
                container.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME &&
                name.asString() == RESULT_NAME
    }
}