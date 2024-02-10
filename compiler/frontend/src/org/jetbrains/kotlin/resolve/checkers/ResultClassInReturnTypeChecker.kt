/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.types.KotlinType

class ResultClassInReturnTypeChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val languageVersionSettings = context.languageVersionSettings

        if ((languageVersionSettings.getFeatureSupport(LanguageFeature.InlineClasses) == LanguageFeature.State.ENABLED ||
                    languageVersionSettings.supportsFeature(LanguageFeature.JvmInlineValueClasses)) &&
            languageVersionSettings.supportsFeature(LanguageFeature.AllowResultInReturnType)
        ) return

        if (languageVersionSettings.supportsFeature(LanguageFeature.AllowNullOperatorsForResultAndResultReturnTypeByDefault)) return

        if (declaration !is KtCallableDeclaration || descriptor !is CallableMemberDescriptor) return

        val returnType = descriptor.returnType ?: return
        if (isForbiddenReturnType(returnType, declaration, descriptor)) {
            val typeReferenceOrDeclarationName = declaration.typeReference ?: declaration.nameIdentifier ?: return
            context.trace.reportDiagnosticOnce(Errors.RESULT_CLASS_IN_RETURN_TYPE.on(typeReferenceOrDeclarationName))
        }
    }

    private fun isForbiddenReturnType(
        returnType: KotlinType, declaration: KtDeclaration, declarationDescriptor: DeclarationDescriptor
    ): Boolean {
        if (!returnType.isResultType()) return false

        if (declarationDescriptor is PropertyDescriptor || declarationDescriptor is PropertyGetterDescriptor) {
            if (declaration is KtProperty && declaration.getter?.hasBody() == true) {
                return true
            }

            val visibility = (declarationDescriptor as DeclarationDescriptorWithVisibility).visibility
            return !DescriptorVisibilities.isPrivate(visibility) && visibility != DescriptorVisibilities.LOCAL
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
            container.fqName == StandardNames.RESULT_FQ_NAME.parent() &&
            name == StandardNames.RESULT_FQ_NAME.shortName()
}
