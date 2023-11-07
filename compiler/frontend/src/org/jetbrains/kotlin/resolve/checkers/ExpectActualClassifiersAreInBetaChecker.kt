/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.areExpectActualClassesStable
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtTypeAlias

object ExpectActualClassifiersAreInBetaChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return
        if (context.languageVersionSettings.areExpectActualClassesStable) return
        if (descriptor !is TypeAliasDescriptor && descriptor !is ClassDescriptor) return
        check(declaration is KtClassOrObject || declaration is KtTypeAlias)

        // Common supertype of KtTypeAlias and KtClassOrObject is KtClassLikeDeclaration.
        // Common supertype of TypeAliasDescriptor and ClassDescriptor is ClassifierDescriptorWithTypeParameters.
        // The explicit casts won't be necessary when we start compiling kotlin with K2.
        @Suppress("USELESS_IS_CHECK") // K2 warning suppression, TODO: KT-62472
        check(declaration is KtClassLikeDeclaration)
        @Suppress("USELESS_IS_CHECK") // K2 warning suppression, TODO: KT-62472
        check(descriptor is ClassifierDescriptorWithTypeParameters)

        if (descriptor.isExpect || descriptor.isActual) {
            context.trace.report(Errors.EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING.on(declaration))
        }
    }
}
