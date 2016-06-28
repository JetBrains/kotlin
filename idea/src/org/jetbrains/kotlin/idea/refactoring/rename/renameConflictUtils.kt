/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.explicateAsText
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.refactoring.getThisLabelName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.and
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getAllAccessibleVariables
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

internal fun ResolvedCall<*>.noReceivers() = dispatchReceiver == null && extensionReceiver == null

internal fun PsiNamedElement.renderDescription() = "${UsageViewUtil.getType(this)} '$name'".trim()

internal fun PsiElement.representativeContainer(): PsiNamedElement? =
        when (this) {
            is KtDeclaration -> containingClassOrObject
                                ?: getStrictParentOfType<KtNamedDeclaration>()
                                ?: JavaPsiFacade.getInstance(project).findPackage(getContainingKtFile().packageFqName.asString())
            is PsiMember -> containingClass
            else -> null
        }

internal fun DeclarationDescriptor.canonicalRender(): String = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

internal fun checkRedeclarations(
        descriptor: DeclarationDescriptor,
        newName: String,
        result: MutableList<UsageInfo>
) {
    fun getSiblingWithNewName(): DeclarationDescriptor? {
        if (descriptor is ValueParameterDescriptor) {
            return descriptor.containingDeclaration.valueParameters.firstOrNull { it.name.asString() == newName }
        }

        val containingDescriptor = descriptor.containingDeclaration
        val containingScope = when (containingDescriptor) {
            is ClassDescriptor -> containingDescriptor.unsubstitutedMemberScope
            is PackageFragmentDescriptor -> containingDescriptor.getMemberScope()
            else -> return null
        }
        val descriptorKindFilter = when (descriptor) {
            is ClassDescriptor -> DescriptorKindFilter.CLASSIFIERS
            is PropertyDescriptor -> DescriptorKindFilter.VARIABLES
            else -> return null
        }
        return containingScope.getDescriptorsFiltered(descriptorKindFilter) { it.asString() == newName }.firstOrNull()
    }

    val candidateDescriptor = getSiblingWithNewName() ?: return
    val candidate = (candidateDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() as? KtNamedDeclaration ?: return
    val what = candidate.renderDescription().capitalize()
    val where = candidate.representativeContainer()?.renderDescription() ?: return
    val message = "$what is already declared in $where"
    result += BasicUnresolvableCollisionUsageInfo(candidate, candidate, message)
}

private fun LexicalScope.getRelevantDescriptors(
        declaration: PsiNamedElement,
        name: String
): Collection<DeclarationDescriptor> {
    val nameAsName = Name.identifier(name)
    return when (declaration) {
        is KtProperty, is KtParameter, is PsiField -> getAllAccessibleVariables(nameAsName)
        is KtClassOrObject, is PsiClass -> findClassifier(nameAsName, NoLookupLocation.FROM_IDE).singletonOrEmptyList()
        else -> emptyList()
    }
}

fun reportShadowing(
        declaration: PsiNamedElement,
        elementToBindUsageInfoTo: PsiElement,
        candidateDescriptor: DeclarationDescriptor,
        refElement: PsiElement,
        result: MutableList<UsageInfo>
) {
    val candidate = DescriptorToSourceUtilsIde.getAnyDeclaration(declaration.project, candidateDescriptor) as? PsiNamedElement ?: return
    val message = "${declaration.renderDescription().capitalize()} will be shadowed by ${candidate.renderDescription()}"
    result += BasicUnresolvableCollisionUsageInfo(refElement, elementToBindUsageInfoTo, message)
}

private fun checkUsagesRetargeting(
        elementToBindUsageInfosTo: PsiElement,
        declaration: PsiNamedElement,
        name: String,
        isNewName: Boolean,
        accessibleDescriptors: Collection<DeclarationDescriptor>,
        originalUsages: MutableList<UsageInfo>,
        newUsages: MutableList<UsageInfo>
) {
    val usageIterator = originalUsages.listIterator()
    while (usageIterator.hasNext()) {
        val usage = usageIterator.next()
        val refElement = usage.element as? KtSimpleNameExpression ?: continue
        val context = refElement.analyze(BodyResolveMode.PARTIAL)
        val scope = refElement
                            .parentsWithSelf
                            .filterIsInstance<KtElement>()
                            .mapNotNull { context[BindingContext.LEXICAL_SCOPE, it] }
                            .firstOrNull()
                    ?: continue

        if (scope.getRelevantDescriptors(declaration, name).isEmpty()) {
            if (declaration !is KtProperty && declaration !is KtParameter) continue
            if (NewDeclarationNameValidator(refElement.parent, refElement, NewDeclarationNameValidator.Target.VARIABLES)(name)) continue
        }

        val psiFactory = KtPsiFactory(declaration)

        val resolvedCall = refElement.getResolvedCall(context)
        if (resolvedCall == null) {
            val typeReference = refElement.getStrictParentOfType<KtTypeReference>() ?: continue
            val referencedClass = context[BindingContext.TYPE, typeReference]?.constructor?.declarationDescriptor ?: continue
            val referencedClassFqName = FqName(IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(referencedClass))
            val newFqName = if (isNewName) referencedClassFqName.parent().child(Name.identifier(name)) else referencedClassFqName
            val fakeVar = psiFactory.createDeclaration<KtProperty>("val __foo__: ${newFqName.asString()}")
            val newContext = fakeVar.analyzeInContext(scope, refElement)
            val referencedClassInNewContext = newContext[BindingContext.TYPE, fakeVar.typeReference!!]?.constructor?.declarationDescriptor
            val candidateText = referencedClassInNewContext?.canonicalRender()
            if (referencedClassInNewContext == null
                || ErrorUtils.isError(referencedClassInNewContext)
                || referencedClass.canonicalRender() == candidateText
                || accessibleDescriptors.any { it.canonicalRender() == candidateText }) {
                usageIterator.set(UsageInfoWithFqNameReplacement(refElement, declaration, newFqName))
            }
            else {
                reportShadowing(declaration, elementToBindUsageInfosTo, referencedClassInNewContext, refElement, newUsages)
            }
            continue
        }

        val callExpression = resolvedCall.call.callElement as? KtExpression ?: continue
        val fullCallExpression = callExpression.getQualifiedExpressionForSelectorOrThis()

        val qualifiedExpression = if (resolvedCall.noReceivers()) {
            val resultingDescriptor = resolvedCall.resultingDescriptor
            val fqName =
                    resultingDescriptor.importableFqName
                    ?: (resultingDescriptor as? ClassifierDescriptor)?.let {
                        FqName(IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(it))
                    }
                    ?: continue
            if (fqName.parent().isRoot) {
                callExpression.copied()
            }
            else {
                psiFactory.createExpressionByPattern("${fqName.parent().asString()}.$0", callExpression)
            }
        }
        else {
            resolvedCall.getExplicitReceiverValue()?.let {
                fullCallExpression.copied()
            }
            ?: resolvedCall.getImplicitReceiverValue()?.let { implicitReceiver ->
                val expectedLabelName = implicitReceiver.declarationDescriptor.getThisLabelName()
                val implicitReceivers = scope.getImplicitReceiversHierarchy()
                val receiversWithExpectedName = implicitReceivers.filter {
                    it.value.type.constructor.declarationDescriptor?.getThisLabelName() == expectedLabelName
                }

                val canQualifyThis = receiversWithExpectedName.isEmpty()
                                     || receiversWithExpectedName.size == 1 && (declaration !is KtClassOrObject || expectedLabelName != name)
                if (canQualifyThis) {
                    psiFactory.createExpressionByPattern("${implicitReceiver.explicateAsText()}.$0", callExpression)
                }
                else {
                    val defaultReceiverClassText =
                            implicitReceivers.firstOrNull()?.value?.type?.constructor?.declarationDescriptor?.canonicalRender()
                    val canInsertUnqualifiedThis = accessibleDescriptors.any { it.canonicalRender() == defaultReceiverClassText }
                    if (canInsertUnqualifiedThis) {
                        psiFactory.createExpressionByPattern("this.$0", callExpression)
                    }
                    else {
                        callExpression.copied()
                    }
                }
            }
            ?: continue
        }

        val newCallee = qualifiedExpression.getQualifiedElementSelector() as? KtSimpleNameExpression ?: continue
        if (isNewName) {
            newCallee.getReferencedNameElement().replace(psiFactory.createNameIdentifier(name))
        }

        val newContext = qualifiedExpression.analyzeInContext(scope, refElement)

        val newResolvedCall = newCallee.getResolvedCall(newContext)
        val candidateText = newResolvedCall?.candidateDescriptor?.getImportableDescriptor()?.canonicalRender()

        if (newResolvedCall != null
            && !accessibleDescriptors.any { it.canonicalRender() == candidateText }
            && resolvedCall.candidateDescriptor.canonicalRender() != candidateText) {
            reportShadowing(declaration, elementToBindUsageInfosTo, newResolvedCall.candidateDescriptor, refElement, newUsages)
            continue
        }

        usageIterator.set(UsageInfoWithReplacement(fullCallExpression, declaration, qualifiedExpression))
    }
}

internal fun checkOriginalUsagesRetargeting(
        declaration: KtNamedDeclaration,
        newName: String,
        originalUsages: MutableList<UsageInfo>,
        newUsages: MutableList<UsageInfo>
) {
    val accessibleDescriptors = declaration.getResolutionScope().getRelevantDescriptors(declaration, newName)
    checkUsagesRetargeting(declaration, declaration, newName, true, accessibleDescriptors, originalUsages, newUsages)
}

internal fun checkNewNameUsagesRetargeting(
        declaration: KtNamedDeclaration,
        newName: String,
        newUsages: MutableList<UsageInfo>
) {
    val currentName = declaration.name ?: return
    val descriptor = declaration.resolveToDescriptor()

    if (declaration is KtParameter && !declaration.hasValOrVar()) {
        val ownerFunction = declaration.ownerFunction
        val searchScope = (if (ownerFunction is KtPrimaryConstructor) ownerFunction.containingClassOrObject else ownerFunction) ?: return

        val usagesByCandidate = LinkedHashMap<PsiElement, MutableList<UsageInfo>>()

        searchScope.accept(
                object: KtTreeVisitorVoid() {
                    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                        if (expression.getReferencedName() != newName) return
                        val ref = expression.mainReference
                        val candidate = ref.resolve() as? PsiNamedElement ?: return
                        usagesByCandidate.getOrPut(candidate) { SmartList() }.add(MoveRenameUsageInfo(ref, candidate))
                    }
                }
        )

        for ((candidate, usages) in usagesByCandidate) {
            checkUsagesRetargeting(candidate, declaration, currentName, false, listOf(descriptor), usages, newUsages)
            usages.filterIsInstanceTo<KtResolvableCollisionUsageInfo, MutableList<UsageInfo>>(newUsages)
        }

        return
    }

    for (candidateDescriptor in declaration.getResolutionScope().getRelevantDescriptors(declaration, newName)) {
        val candidate = DescriptorToSourceUtilsIde.getAnyDeclaration(declaration.project, candidateDescriptor) as? PsiNamedElement ?: continue
        val usages = ReferencesSearch
                .search(candidate, candidate.useScope.restrictToKotlinSources() and declaration.useScope)
                .mapTo(SmartList<UsageInfo>()) { MoveRenameUsageInfo(it, candidate) }
        checkUsagesRetargeting(candidate, declaration, currentName, false, listOf(descriptor), usages, newUsages)
        usages.filterIsInstanceTo<KtResolvableCollisionUsageInfo, MutableList<UsageInfo>>(newUsages)
    }
}