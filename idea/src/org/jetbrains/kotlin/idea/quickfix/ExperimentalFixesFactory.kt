/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module

object ExperimentalFixesFactory : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val element = diagnostic.psiElement
        val containingDeclaration: KtDeclaration = element.getParentOfTypesAndPredicate(
            true,
            KtDeclarationWithBody::class.java,
            KtClassOrObject::class.java,
            KtProperty::class.java,
            KtTypeAlias::class.java
        ) {
            !KtPsiUtil.isLocal(it)
        } ?: return emptyList()

        val factory = diagnostic.factory
        val annotationFqName = when (factory) {
            EXPERIMENTAL_API_USAGE -> EXPERIMENTAL_API_USAGE.cast(diagnostic).a
            EXPERIMENTAL_API_USAGE_ERROR -> EXPERIMENTAL_API_USAGE_ERROR.cast(diagnostic).a
            EXPERIMENTAL_OVERRIDE -> EXPERIMENTAL_OVERRIDE.cast(diagnostic).a
            EXPERIMENTAL_OVERRIDE_ERROR -> EXPERIMENTAL_OVERRIDE_ERROR.cast(diagnostic).a
            else -> null
        } ?: return emptyList()

        val moduleDescriptor = containingDeclaration.resolveToDescriptorIfAny()?.module ?: return emptyList()
        val annotationClassDescriptor = moduleDescriptor.resolveClassByFqName(
            annotationFqName, NoLookupLocation.FROM_IDE
        ) ?: return emptyList()
        val applicableTargets = AnnotationChecker.applicableTargetSet(annotationClassDescriptor) ?: KotlinTarget.DEFAULT_TARGET_SET

        val context = when (element) {
            is KtElement -> element.analyze()
            else -> containingDeclaration.analyze()
        }

        fun isApplicableTo(declaration: KtDeclaration, applicableTargets: Set<KotlinTarget>): Boolean {
            val actualTargetList = AnnotationChecker.getDeclarationSiteActualTargetList(
                declaration, declaration.toDescriptor() as? ClassDescriptor, context
            )
            return actualTargetList.any { it in applicableTargets }
        }

        val result = mutableListOf<IntentionAction>()
        run {
            val suffix = " to '${containingDeclaration.name}'"
            if (isApplicableTo(containingDeclaration, applicableTargets)) {
                result.add(AddAnnotationFix(containingDeclaration, annotationFqName, suffix))
            }
            result.add(
                AddAnnotationFix(
                    containingDeclaration, ExperimentalUsageChecker.OPT_IN_FQ_NAME, suffix, annotationFqName
                )
            )
        }
        if (containingDeclaration is KtCallableDeclaration) {
            val containingClassOrObject = containingDeclaration.containingClassOrObject
            if (containingClassOrObject != null) {
                val suffix = " to containing class '${containingClassOrObject.name}'"
                if (isApplicableTo(containingClassOrObject, applicableTargets)) {
                    result.add(AddAnnotationFix(containingClassOrObject, annotationFqName, suffix))
                } else {
                    result.add(
                        AddAnnotationFix(
                            containingClassOrObject, ExperimentalUsageChecker.OPT_IN_FQ_NAME, suffix, annotationFqName
                        )
                    )
                }
            }
        }
        val containingFile = containingDeclaration.containingKtFile
        val module = containingFile.module
        if (module != null) {
            result.add(
                MakeModuleExperimentalFix(containingFile, module, annotationFqName)
            )
        }

        return result
    }
}
