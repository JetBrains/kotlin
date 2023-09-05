/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.resolve.calls.mpp.ActualTypealiasToSpecialAnnotationUtils.isAnnotationProhibitedInActualTypeAlias
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

internal object ActualTypealiasToSpecialAnnotationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) return
        if (declaration !is KtTypeAlias || descriptor !is TypeAliasDescriptor || !descriptor.isActual) {
            return
        }
        val classDescriptor = descriptor.classDescriptor ?: return
        if (classDescriptor.kind != ClassKind.ANNOTATION_CLASS) {
            return
        }
        val classId = classDescriptor.classId ?: return
        if (isAnnotationProhibitedInActualTypeAlias(classId)) {
            context.trace.report(Errors.ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION.on(declaration, classId))
        }
    }
}