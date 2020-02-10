/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.SearchScope
import com.intellij.slicer.JavaSliceUsage
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.processAllExactUsages
import org.jetbrains.kotlin.idea.findUsages.processAllUsages
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.*

abstract class Slicer(
    protected val element: KtExpression,
    protected val processor: Processor<SliceUsage>,
    protected val parentUsage: KotlinSliceUsage
) {
    abstract fun processChildren()

    protected class PseudocodeCache {
        private val computedPseudocodes = HashMap<KtElement, Pseudocode>()

        operator fun get(element: KtElement): Pseudocode? {
            val container = element.containingDeclarationForPseudocode ?: return null
            return computedPseudocodes.getOrPut(container) {
                container.getContainingPseudocode(container.analyzeWithContent())?.apply { computedPseudocodes[container] = this }
                    ?: return null
            }
        }
    }

    protected val pseudocodeCache = PseudocodeCache()

    protected fun PsiElement.passToProcessor(
        lambdaLevel: Int = parentUsage.lambdaLevel,
        forcedExpressionMode: Boolean = false
    ) {
        processor.process(KotlinSliceUsage(this, parentUsage, lambdaLevel, forcedExpressionMode))
    }

    protected fun KtFunction.processCalls(scope: SearchScope, includeOverriders: Boolean, usageProcessor: (UsageInfo) -> Unit) {
        val options = KotlinFunctionFindUsagesOptions(project).apply {
            isSearchForTextOccurrences = false
            isSkipImportStatements = true
            searchScope = scope.intersectWith(useScope)
        }

        val descriptor = unsafeResolveToDescriptor() as? CallableMemberDescriptor ?: return
        val superDescriptors = if (includeOverriders)
            descriptor.getDeepestSuperDeclarations()
        else
            listOf(descriptor) + DescriptorUtils.getAllOverriddenDeclarations(descriptor)

        for (superDescriptor in superDescriptors) {
            val declaration = superDescriptor.originalSource.getPsi() ?: continue
            when (declaration) {
                is KtDeclaration -> {
                    if (includeOverriders) {
                        declaration.processAllUsages(options, usageProcessor)
                    } else {
                        declaration.processAllExactUsages(options, usageProcessor)
                    }
                }

                is PsiMethod -> {
                    // todo: work around the bug in JavaSliceProvider.transform()
                    processor.process(JavaSliceUsage.createRootUsage(declaration, parentUsage.params))
                }

                else -> {
                    if (declaration in scope) {
                        declaration.passToProcessor()
                    }
                }
            }
        }
    }

    protected enum class AccessKind {
        READ_ONLY, WRITE_ONLY, WRITE_WITH_OPTIONAL_READ, READ_OR_WRITE
    }

    protected fun processVariableAccesses(
        declaration: KtCallableDeclaration,
        scope: SearchScope,
        kind: AccessKind,
        usageProcessor: (UsageInfo) -> Unit
    ) {
        val allDeclarations = mutableListOf(declaration)
        val descriptor = declaration.unsafeResolveToDescriptor()
        if (descriptor is CallableMemberDescriptor) {
            DescriptorUtils.getAllOverriddenDeclarations(descriptor).mapNotNullTo(allDeclarations) {
                it.originalSource.getPsi() as? KtCallableDeclaration
            }
        }

        for (aDeclaration in allDeclarations) {
            aDeclaration.processAllExactUsages(
                KotlinPropertyFindUsagesOptions(aDeclaration.project).apply {
                    isReadAccess = kind == AccessKind.READ_ONLY || kind == AccessKind.READ_OR_WRITE
                    isWriteAccess = kind == AccessKind.WRITE_ONLY || kind == AccessKind.WRITE_WITH_OPTIONAL_READ || kind == AccessKind.READ_OR_WRITE
                    isReadWriteAccess = kind == AccessKind.WRITE_WITH_OPTIONAL_READ || kind == AccessKind.READ_OR_WRITE
                    isSearchForTextOccurrences = false
                    isSkipImportStatements = true
                    searchScope = scope.intersectWith(aDeclaration.useScope)
                },
                usageProcessor
            )
        }
    }

    protected fun KtParameter.canProcess() = !isVarArg

    protected val DeclarationDescriptorWithSource.originalSource: SourceElement
        get() {
            var descriptor = this
            while (descriptor.original != descriptor) {
                descriptor = descriptor.original
            }
            return descriptor.source
        }
}
