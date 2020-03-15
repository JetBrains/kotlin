/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.parentOfType
import com.intellij.slicer.JavaSliceUsage
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.processAllExactUsages
import org.jetbrains.kotlin.idea.findUsages.processAllUsages
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.expectedDescriptor
import org.jetbrains.kotlin.idea.util.hasInlineModifier
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

abstract class Slicer(
    protected val element: KtElement,
    protected val processor: Processor<in SliceUsage>,
    protected val parentUsage: KotlinSliceUsage
) {
    abstract fun processChildren(forcedExpressionMode: Boolean)

    protected val analysisScope: SearchScope = parentUsage.scope.toSearchScope()
    protected val mode: KotlinSliceAnalysisMode = parentUsage.mode
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

    protected fun PsiElement.passToProcessor(mode: KotlinSliceAnalysisMode = this@Slicer.mode) {
        processor.process(KotlinSliceUsage(this, parentUsage, mode, false))
    }

    protected fun PsiElement.passToProcessorAsValue(mode: KotlinSliceAnalysisMode = this@Slicer.mode) {
        processor.process(KotlinSliceUsage(this, parentUsage, mode, forcedExpressionMode = true))
    }

    protected fun PsiElement.passToProcessorInCallMode(
        callElement: KtElement,
        mode: KotlinSliceAnalysisMode = this@Slicer.mode,
        withOverriders: Boolean = false
    ) {
        val newMode = when (this) {
            is KtNamedFunction -> this.callMode(callElement, mode)

            is KtParameter -> ownerFunction.callMode(callElement, mode)

            is KtTypeReference -> {
                val declaration = parent
                require(declaration is KtCallableDeclaration)
                require(this == declaration.receiverTypeReference)
                declaration.callMode(callElement, mode)
            }

            else -> mode
        }

        if (withOverriders) {
            passDeclarationToProcessorWithOverriders(newMode)
        } else {
            passToProcessor(newMode)
        }
    }

    protected fun PsiElement.passDeclarationToProcessorWithOverriders(mode: KotlinSliceAnalysisMode = this@Slicer.mode) {
        passToProcessor(mode)

        HierarchySearchRequest(this, analysisScope)
            .searchOverriders()
            .forEach { it.namedUnwrappedElement?.passToProcessor(mode) }

        if (this is KtCallableDeclaration && isExpectDeclaration()) {
            resolveToDescriptorIfAny()
                ?.actualsForExpected()
                ?.forEach {
                    (it as? DeclarationDescriptorWithSource)?.toPsi()?.passToProcessor(mode)
                }
        }
    }

    protected open fun processCalls(
        callable: KtCallableDeclaration,
        includeOverriders: Boolean,
        sliceProducer: SliceProducer,
    ) {
        if (callable is KtFunctionLiteral || callable is KtFunction && callable.name == null) {
            callable.passToProcessorAsValue(mode.withBehaviour(LambdaCallsBehaviour(sliceProducer)))
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

        val descriptor = callable.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return
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
            val declaration = superDescriptor.toPsi() ?: continue
            when (declaration) {
                is KtDeclaration -> {
                    val usageProcessor: (UsageInfo) -> Unit = processor@ { usageInfo ->
                        val element = usageInfo.element ?: return@processor
                        if (element.parentOfType<PsiComment>() != null) return@processor
                        val sliceUsage = KotlinSliceUsage(element, parentUsage, mode, false)
                        sliceProducer.produceAndProcess(sliceUsage, mode, parentUsage, processor)
                    }
                    if (includeOverriders) {
                        declaration.processAllUsages(options, usageProcessor)
                    } else {
                        declaration.processAllExactUsages(options, usageProcessor)
                    }
                }

                is PsiMethod -> {
                    val sliceUsage = JavaSliceUsage.createRootUsage(declaration, parentUsage.params)
                    sliceProducer.produceAndProcess(sliceUsage, mode, parentUsage, processor)
                }

                else -> {
                    val sliceUsage = KotlinSliceUsage(declaration, parentUsage, mode, false)
                    sliceProducer.produceAndProcess(sliceUsage, mode, parentUsage, processor)
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
                it.toPsi() as? KtCallableDeclaration
            }

        }

        allDeclarations.forEach {
            it.processAllExactUsages(options) { usageInfo ->
                if (!shouldIgnoreVariableUsage(usageInfo)) {
                    usageProcessor.invoke(usageInfo)
                }
            }
        }
    }

    // ignore parameter usages in function contract
    private fun shouldIgnoreVariableUsage(usage: UsageInfo): Boolean {
        val element = usage.element ?: return true
        return element.parents.any {
            it is KtCallExpression &&
                    (it.calleeExpression as? KtSimpleNameExpression)?.getReferencedName() == "contract" &&
                    it.resolveToCall()?.resultingDescriptor?.fqNameOrNull()?.asString() == "kotlin.contracts.contract"
        }
    }

    protected fun canProcessParameter(parameter: KtParameter) = !parameter.isVarArg

    protected fun processExtensionReceiverUsages(
        declaration: KtCallableDeclaration,
        body: KtExpression?,
        mode: KotlinSliceAnalysisMode,
    ) {
        if (body == null) return
        //TODO: overriders
        val resolutionFacade = declaration.getResolutionFacade()
        val callableDescriptor = declaration.resolveToDescriptorIfAny(resolutionFacade) as? CallableDescriptor ?: return
        val extensionReceiver = callableDescriptor.extensionReceiverParameter ?: return

        body.forEachDescendantOfType<KtThisExpression> { thisExpression ->
            val receiverDescriptor = thisExpression.resolveToCall(resolutionFacade)?.resultingDescriptor
            if (receiverDescriptor == extensionReceiver) {
                thisExpression.passToProcessor(mode)
            }
        }

        // process implicit receiver usages
        val pseudocode = pseudocodeCache[body]
        if (pseudocode != null) {
            for (instruction in pseudocode.instructions) {
                if (instruction is MagicInstruction && instruction.kind == MagicKind.IMPLICIT_RECEIVER) {
                    val receiverPseudoValue = instruction.outputValue
                    pseudocode.getUsages(receiverPseudoValue).forEach { receiverUseInstruction ->
                        if (receiverUseInstruction is KtElementInstruction) {
                            receiverPseudoValue.processIfReceiverValue(
                                receiverUseInstruction,
                                mode,
                                filter = { receiverValue, resolvedCall ->
                                    receiverValue == resolvedCall.extensionReceiver &&
                                            (receiverValue as? ImplicitReceiver)?.declarationDescriptor == callableDescriptor
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    protected fun PseudoValue.processIfReceiverValue(
        instruction: KtElementInstruction,
        mode: KotlinSliceAnalysisMode,
        filter: (ReceiverValue, ResolvedCall<out CallableDescriptor>) -> Boolean = { _, _ -> true }
    ): Boolean {
        val receiverValue = (instruction as? InstructionWithReceivers)?.receiverValues?.get(this) ?: return false
        val resolvedCall = instruction.element.resolveToCall() ?: return true
        if (!filter(receiverValue, resolvedCall)) return true

        val descriptor = resolvedCall.resultingDescriptor

        if (descriptor.isImplicitInvokeFunction()) {
            when (receiverValue) {
                resolvedCall.dispatchReceiver -> {
                    if (mode.currentBehaviour is LambdaCallsBehaviour) {
                        instruction.element.passToProcessor(mode)
                    }
                }

                resolvedCall.extensionReceiver -> {
                    val dispatchReceiver = resolvedCall.dispatchReceiver ?: return true
                    val dispatchReceiverPseudoValue = instruction.receiverValues.entries
                        .singleOrNull { it.value == dispatchReceiver }?.key
                        ?: return true
                    val createdAt = dispatchReceiverPseudoValue.createdAt
                    val accessedDescriptor = (createdAt as ReadValueInstruction?)?.target?.accessedDescriptor
                    if (accessedDescriptor is VariableDescriptor) {
                        accessedDescriptor.toPsi()?.passToProcessor(mode.withBehaviour(LambdaReceiverInflowBehaviour))
                    }
                }
            }
        } else {
            if (receiverValue == resolvedCall.extensionReceiver) {
                (descriptor.toPsi() as? KtCallableDeclaration)?.receiverTypeReference
                    ?.passToProcessorInCallMode(instruction.element, mode)
            }
        }

        return true
    }

    protected fun DeclarationDescriptor.toPsi(): PsiElement? {
        return descriptorToPsi(this, project, analysisScope)
    }

    companion object {
        protected fun KtDeclaration?.callMode(callElement: KtElement, defaultMode: KotlinSliceAnalysisMode): KotlinSliceAnalysisMode {
            return if (this is KtNamedFunction && hasInlineModifier())
                defaultMode.withInlineFunctionCall(callElement, this)
            else
                defaultMode
        }

        fun descriptorToPsi(descriptor: DeclarationDescriptor, project: Project, analysisScope: SearchScope): PsiElement? {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) ?: return null
            if (analysisScope.contains(declaration)) return declaration.navigationElement

            // we ignore access scope for inline declarations
            val isInline = when (declaration) {
                is KtNamedFunction -> declaration.hasInlineModifier()
                is KtParameter -> declaration.ownerFunction?.hasInlineModifier() == true
                else -> false
            }
            return if (isInline) declaration.navigationElement else null
        }
    }
}

fun CallableDescriptor.isImplicitInvokeFunction(): Boolean {
    if (this !is FunctionDescriptor) return false
    if (!isOperator) return false
    if (name != OperatorNameConventions.INVOKE) return false
    return source.getPsi() == null
}