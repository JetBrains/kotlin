/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.SearchScope
import com.intellij.slicer.JavaSliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.handlers.SliceUsageProcessor
import org.jetbrains.kotlin.idea.findUsages.processAllExactUsages
import org.jetbrains.kotlin.idea.findUsages.processAllUsages
import org.jetbrains.kotlin.idea.util.expectedDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.*

abstract class Slicer(
    protected val element: KtElement,
    protected val processor: SliceUsageProcessor,
    protected val parentUsage: KotlinSliceUsage
) {
    abstract fun processChildren(forcedExpressionMode: Boolean)

    protected val analysisScope: SearchScope = parentUsage.scope.toSearchScope()
    protected val behaviour: KotlinSliceUsage.SpecialBehaviour? = parentUsage.behaviour
    protected val project = element.project

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
        behaviour: KotlinSliceUsage.SpecialBehaviour? = this@Slicer.behaviour,
        forcedExpressionMode: Boolean = false,
    ) {
        processor.process(KotlinSliceUsage(this, parentUsage, behaviour, forcedExpressionMode))
    }

    protected fun PsiElement.passToProcessorAsValue(behaviour: KotlinSliceUsage.SpecialBehaviour? = this@Slicer.behaviour) {
        passToProcessor(behaviour, forcedExpressionMode = true)
    }

    protected fun processCalls(
        callable: KtCallableDeclaration,
        includeOverriders: Boolean,
        sliceProducer: SliceProducer
    ) {
        if (callable is KtFunctionLiteral || callable is KtFunction && callable.name == null) {
            callable.passToProcessorAsValue(LambdaCallsBehaviour(sliceProducer, behaviour))
            return
        }

        val options = when (callable) {
            is KtFunction -> {
                KotlinFunctionFindUsagesOptions(project).apply {
                    isSearchForTextOccurrences = false
                    isSkipImportStatements = true
                    searchScope = analysisScope
                }
            }

            is KtProperty -> {
                KotlinPropertyFindUsagesOptions(project).apply {
                    isSearchForTextOccurrences = false
                    isSkipImportStatements = true
                    searchScope = analysisScope
                }
            }

            else -> return
        }

        val descriptor = callable.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableMemberDescriptor ?: return
        val superDescriptors = if (includeOverriders) {
            descriptor.getDeepestSuperDeclarations()
        } else {
            mutableListOf<CallableMemberDescriptor>().apply {
                add(descriptor)
                addAll(DescriptorUtils.getAllOverriddenDeclarations(descriptor))
                if (descriptor.isActual) {
                    addIfNotNull(descriptor.expectedDescriptor() as CallableMemberDescriptor?)
                }
            }
        }

        for (superDescriptor in superDescriptors) {
            val declaration = superDescriptor.originalSource.getPsi() ?: continue
            when (declaration) {
                is KtDeclaration -> {
                    val usageProcessor: (UsageInfo) -> Unit = processor@ { usageInfo ->
                        val element = usageInfo.element ?: return@processor
                        val sliceUsage = KotlinSliceUsage(element, parentUsage, behaviour, false)
                        sliceProducer.produceAndProcess(sliceUsage, behaviour, parentUsage, processor)
                    }
                    if (includeOverriders) {
                        declaration.processAllUsages(options, usageProcessor)
                    } else {
                        declaration.processAllExactUsages(options, usageProcessor)
                    }
                }

                is PsiMethod -> {
                    val sliceUsage = JavaSliceUsage.createRootUsage(declaration, parentUsage.params)
                    sliceProducer.produceAndProcess(sliceUsage, behaviour, parentUsage, processor)
                }

                else -> {
                    val sliceUsage = KotlinSliceUsage(declaration, parentUsage, behaviour, false)
                    sliceProducer.produceAndProcess(sliceUsage, behaviour, parentUsage, processor)
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
        val options = KotlinPropertyFindUsagesOptions(project).apply {
            isReadAccess = kind == AccessKind.READ_ONLY || kind == AccessKind.READ_OR_WRITE
            isWriteAccess =
                kind == AccessKind.WRITE_ONLY || kind == AccessKind.WRITE_WITH_OPTIONAL_READ || kind == AccessKind.READ_OR_WRITE
            isReadWriteAccess = kind == AccessKind.WRITE_WITH_OPTIONAL_READ || kind == AccessKind.READ_OR_WRITE
            isSearchForTextOccurrences = false
            isSkipImportStatements = true
            searchScope = scope
        }

        val allDeclarations = mutableListOf(declaration)
        val descriptor = declaration.resolveToDescriptorIfAny()
        if (descriptor is CallableMemberDescriptor) {
            val additionalDescriptors = if (descriptor.isActual) {
                listOfNotNull(descriptor.expectedDescriptor() as? CallableMemberDescriptor)
            } else {
                DescriptorUtils.getAllOverriddenDeclarations(descriptor)
            }
            additionalDescriptors.mapNotNullTo(allDeclarations) {
                it.originalSource.getPsi() as? KtCallableDeclaration
            }

        }

        allDeclarations.forEach { it.processAllExactUsages(options, usageProcessor) }
    }

    protected fun canProcessParameter(parameter: KtParameter) = !parameter.isVarArg

    protected companion object {
        val DeclarationDescriptorWithSource.originalSource: SourceElement
            get() {
                var descriptor = this
                while (descriptor.original != descriptor) {
                    descriptor = descriptor.original
                }
                return descriptor.source
            }
    }
}
