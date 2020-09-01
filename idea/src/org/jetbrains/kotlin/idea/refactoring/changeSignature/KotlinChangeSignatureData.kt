/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.changeSignature.MethodDescriptor
import com.intellij.refactoring.changeSignature.OverriderUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.DescriptorUtils

class KotlinChangeSignatureData(
    override val baseDescriptor: CallableDescriptor,
    override val baseDeclaration: PsiElement,
    private val descriptorsForSignatureChange: Collection<CallableDescriptor>
) : KotlinMethodDescriptor {
    private val parameters: List<KotlinParameterInfo>
    override val receiver: KotlinParameterInfo?

    init {
        receiver = createReceiverInfoIfNeeded()

        val valueParameters = when (baseDeclaration) {
            is KtFunction -> baseDeclaration.valueParameters
            is KtClass -> baseDeclaration.primaryConstructorParameters
            else -> null
        }
        parameters = baseDescriptor.valueParameters
            .mapTo(receiver?.let { arrayListOf(it) } ?: arrayListOf()) { parameterDescriptor ->
                val jetParameter = valueParameters?.get(parameterDescriptor.index)
                val parameterType = parameterDescriptor.type
                val parameterTypeText = jetParameter?.typeReference?.text
                    ?: IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(parameterType)
                KotlinParameterInfo(
                    callableDescriptor = baseDescriptor,
                    originalIndex = parameterDescriptor.index,
                    name = parameterDescriptor.name.asString().quoteIfNeeded(),
                    originalTypeInfo = KotlinTypeInfo(false, parameterType, parameterTypeText),
                    defaultValueForParameter = jetParameter?.defaultValue,
                    valOrVar = jetParameter?.valOrVarKeyword.toValVar()
                )
            }
    }

    private fun createReceiverInfoIfNeeded(): KotlinParameterInfo? {
        val receiverType = baseDescriptor.extensionReceiverParameter?.type ?: return null
        val receiverName = suggestReceiverNames(baseDeclaration.project, baseDescriptor).first()
        val receiverTypeText = (baseDeclaration as? KtCallableDeclaration)?.receiverTypeReference?.text
            ?: IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(receiverType)
        return KotlinParameterInfo(
            callableDescriptor = baseDescriptor,
            name = receiverName,
            originalTypeInfo = KotlinTypeInfo(false, receiverType, receiverTypeText)
        )
    }

    override val original: KotlinMethodDescriptor
        get() = this

    override val primaryCallables: Collection<KotlinCallableDefinitionUsage<PsiElement>> by lazy {
        descriptorsForSignatureChange.map {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(baseDeclaration.project, it)
            assert(declaration != null) { "No declaration found for $baseDescriptor" }
            KotlinCallableDefinitionUsage(declaration!!, it, null, null)
        }
    }

    override val originalPrimaryCallable: KotlinCallableDefinitionUsage<PsiElement> by lazy {
        primaryCallables.first { it.declaration == baseDeclaration }
    }

    override val affectedCallables: Collection<UsageInfo> by lazy {
        val results = hashSetOf<UsageInfo>()

        results += primaryCallables
        for (primaryCallable in primaryCallables) {
            val primaryDeclaration = primaryCallable.declaration as? KtDeclaration ?: continue
            collectMembers(primaryDeclaration, primaryCallable, results)
        }

        results
    }

    private fun collectMembers(
        declaration: KtDeclaration,
        primaryFunction: KotlinCallableDefinitionUsage<PsiElement>,
        results: MutableCollection<UsageInfo>
    ) {
        if (declaration.isEffectivelyActual()) {
            declaration.liftToExpected()?.let { collectExpectActualMembers(it, primaryFunction, results) }
        }

        if (declaration.isExpectDeclaration()) for (it in declaration.actualsForExpected()) collectExpectActualMembers(
            it,
            primaryFunction,
            results
        )

        if (declaration !is KtCallableDeclaration) return

        declaration.forEachOverridingElement { baseElement, overridingElement ->
            val currentDeclaration = overridingElement.namedUnwrappedElement
            results += when (currentDeclaration) {
                is KtDeclaration -> {
                    val overridingDescriptor = currentDeclaration.unsafeResolveToDescriptor() as CallableDescriptor
                    KotlinCallableDefinitionUsage(
                        currentDeclaration,
                        overridingDescriptor,
                        primaryFunction,
                        null,
                        canDropOverride = false
                    )
                }

                is PsiMethod -> {
                    val baseMethod = baseElement as? PsiMethod ?: return@forEachOverridingElement true
                    OverriderUsageInfo(currentDeclaration, baseMethod, true, true, true)
                }

                else -> return@forEachOverridingElement true
            }

            true
        }
    }

    private fun collectExpectActualMembers(
        it: KtDeclaration,
        primaryFunction: KotlinCallableDefinitionUsage<PsiElement>,
        results: MutableCollection<UsageInfo>
    ) {
        val callableDescriptor = when (val descriptor = it.unsafeResolveToDescriptor()) {
            is CallableDescriptor -> descriptor
            is ClassDescriptor -> descriptor.unsubstitutedPrimaryConstructor ?: return
            else -> return
        }

        val usage = KotlinCallableDefinitionUsage<PsiElement>(
            it,
            callableDescriptor,
            primaryFunction,
            null,
            canDropOverride = false
        )

        if (results.add(usage)) collectMembers(it, primaryFunction, results)
    }

    override fun getParameters(): List<KotlinParameterInfo> = parameters

    override fun getName() = when (baseDescriptor) {
        is ConstructorDescriptor -> baseDescriptor.containingDeclaration.name.asString()
        is AnonymousFunctionDescriptor -> ""
        else -> baseDescriptor.name.asString()
    }

    override fun getParametersCount(): Int = baseDescriptor.valueParameters.size

    override fun getVisibility(): DescriptorVisibility = baseDescriptor.visibility

    override fun getMethod(): PsiElement = baseDeclaration

    override fun canChangeVisibility(): Boolean {
        if (DescriptorUtils.isLocal(baseDescriptor)) return false
        val parent = baseDescriptor.containingDeclaration
        return !(baseDescriptor is AnonymousFunctionDescriptor || parent is ClassDescriptor && parent.kind == ClassKind.INTERFACE)
    }

    override fun canChangeParameters() = true

    override fun canChangeName() = !(baseDescriptor is ConstructorDescriptor || baseDescriptor is AnonymousFunctionDescriptor)

    override fun canChangeReturnType(): MethodDescriptor.ReadWriteOption =
        if (baseDescriptor is ConstructorDescriptor) MethodDescriptor.ReadWriteOption.None else MethodDescriptor.ReadWriteOption.ReadWrite
}
