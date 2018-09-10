/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

class ResultClassInReturnTypeChecker : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.getFlag(AnalysisFlag.allowResultReturnType)) return

        if (declaration !is KtCallableDeclaration || descriptor !is CallableMemberDescriptor) return

        val returnType = descriptor.returnType ?: return
        if (isForbiddenReturnType(returnType, descriptor)) {
            val typeReferenceOrDeclarationName = declaration.typeReference ?: declaration.nameIdentifier ?: return
            context.trace.reportDiagnosticOnce(Errors.RESULT_CLASS_IN_RETURN_TYPE.on(typeReferenceOrDeclarationName))
        }
    }

    private fun isForbiddenReturnType(returnType: KotlinType, declarationDescriptor: DeclarationDescriptor): Boolean {
        if (!returnType.isResultType()) return false

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
}

internal fun KotlinType.isResultType(): Boolean {
    return this.constructor.declarationDescriptor?.isResultClass() == true
}

private fun DeclarationDescriptor.isResultClass(): Boolean {
    val container = containingDeclaration ?: return false
    return container is PackageFragmentDescriptor &&
            container.fqName == DescriptorUtils.RESULT_FQ_NAME.parent() &&
            name == DescriptorUtils.RESULT_FQ_NAME.shortName()
}
