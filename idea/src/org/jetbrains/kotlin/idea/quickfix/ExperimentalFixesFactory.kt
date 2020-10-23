/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
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

        val annotationFqName = when (diagnostic.factory) {
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
            val kind = AddAnnotationFix.Kind.Declaration(containingDeclaration.name)
            if (isApplicableTo(containingDeclaration, applicableTargets)) {
                result.add(AddAnnotationFix(containingDeclaration, annotationFqName, kind))
            }
            result.add(
                AddAnnotationFix(
                    containingDeclaration, moduleDescriptor.OPT_IN_FQ_NAME, kind, annotationFqName,
                    containingDeclaration.findAnnotation(moduleDescriptor.OPT_IN_FQ_NAME)?.createSmartPointer()
                )
            )
        }
        if (containingDeclaration is KtCallableDeclaration) {
            val containingClassOrObject = containingDeclaration.containingClassOrObject
            if (containingClassOrObject != null) {
                val kind = AddAnnotationFix.Kind.ContainingClass(containingClassOrObject.name)
                if (isApplicableTo(containingClassOrObject, applicableTargets)) {
                    result.add(AddAnnotationFix(containingClassOrObject, annotationFqName, kind))
                } else {
                    result.add(
                        AddAnnotationFix(
                            containingClassOrObject, moduleDescriptor.OPT_IN_FQ_NAME, kind, annotationFqName,
                            containingDeclaration.findAnnotation(moduleDescriptor.OPT_IN_FQ_NAME)?.createSmartPointer()
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

    private val ModuleDescriptor.OPT_IN_FQ_NAME: FqName
        get() = ExperimentalUsageChecker.OPT_IN_FQ_NAME.takeIf { fqNameIsExisting(it) }
            ?: ExperimentalUsageChecker.OLD_USE_EXPERIMENTAL_FQ_NAME


    fun ModuleDescriptor.fqNameIsExisting(fqName: FqName): Boolean = resolveClassByFqName(fqName, NoLookupLocation.FROM_IDE) != null
}
