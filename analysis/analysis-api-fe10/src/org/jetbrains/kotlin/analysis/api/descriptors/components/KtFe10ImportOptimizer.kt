/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizer
import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.analysis.api.descriptors.CallTypeAndReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.references.fe10.Fe10SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.references.fe10.KtFe10InvokeFunctionReference
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.types.error.ErrorFunctionDescriptor

internal class KtFe10ImportOptimizer(
    override val analysisSession: KtFe10AnalysisSession
) : KtImportOptimizer(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun analyseImports(file: KtFile): KtImportOptimizerResult = withValidityAssertion {
        val imports = file.importDirectives

        val unusedImports = mutableSetOf<KtImportDirective>()
        val visitor = CollectUsedDescriptorsVisitor(file)
        file.accept(visitor)
        val optimizerData = visitor.data

        val explicitlyImportedFqNames = mutableSetOf<FqName>()

        for (import in imports) {
            val fqName = import.importedFqName ?: continue
            if (import.alias == null) {
                explicitlyImportedFqNames += fqName
            }
        }

        val parentFqNames = mutableSetOf<FqName>()
        val importPaths = HashSet<ImportPath>(file.importDirectives.size)
        val fqNames = optimizerData.namesToImport

        for ((_, fqName) in optimizerData.descriptorsToImport) {
            // we don't add parents of explicitly imported fq-names because such imports are not needed
            if (fqName in explicitlyImportedFqNames) continue
            val parentFqName = fqName.parent()
            if (!parentFqName.isRoot) {
                parentFqNames.add(parentFqName)
            }
        }

        val invokeFunctionCallFqNames = optimizerData.references.flatMap { reference ->
            val element = reference.element
            val context = analyze(element) {
                (this as KtFe10AnalysisSession).analysisContext.analyze(element, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
            }
            val mainReference = (element as? KtCallExpression)?.mainReference as? KtFe10InvokeFunctionReference
            mainReference?.getTargetDescriptors(context)?.map { it.fqNameSafe }.orEmpty()
        }

        for (import in imports) {
            val importPath = import.importPath ?: continue

            val isUsed = when {
                importPath.importedName in optimizerData.unresolvedNames -> true
                !importPaths.add(importPath) -> false
                importPath.isAllUnder -> optimizerData.unresolvedNames.isNotEmpty() || importPath.fqName in parentFqNames
                importPath.fqName in fqNames -> importPath.importedName?.let { it in fqNames.getValue(importPath.fqName) } ?: false
                importPath.fqName in invokeFunctionCallFqNames -> true
                // case for type alias
                else -> import.targetDescriptors().firstOrNull()?.let { it.fqNameSafe in fqNames } ?: false
            }

            if (!isUsed) {
                unusedImports += import
            }
        }

        return KtImportOptimizerResult(unusedImports)
    }

    private data class ImportableDescriptor(
        val descriptor: DeclarationDescriptor,
        val fqName: FqName,
    )

    private interface AbstractReference {
        val element: KtElement
        val dependsOnNames: Collection<Name>
    }

    private class InputData(
        val descriptorsToImport: Set<ImportableDescriptor>,
        val namesToImport: Map<FqName, Set<Name>>,
        val references: Collection<AbstractReference>,
        val unresolvedNames: Set<Name>,
    )

    private class CollectUsedDescriptorsVisitor(file: KtFile) : KtVisitorVoid() {
        private val currentPackageName = file.packageFqName
        private val aliases: Map<FqName, List<Name>> = file.importDirectives
            .asSequence()
            .filter { !it.isAllUnder && it.alias != null }
            .mapNotNull { it.importPath }
            .groupBy(keySelector = { it.fqName }, valueTransform = { it.importedName as Name })

        private val descriptorsToImport = hashSetOf<ImportableDescriptor>()
        private val namesToImport = hashMapOf<FqName, MutableSet<Name>>()
        private val abstractRefs = ArrayList<AbstractReference>()
        private val unresolvedNames = hashSetOf<Name>()

        val data get() = InputData(descriptorsToImport, namesToImport, abstractRefs, unresolvedNames)

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitImportList(importList: KtImportList) {
        }

        override fun visitPackageDirective(directive: KtPackageDirective) {
        }

        override fun visitKtElement(element: KtElement) {
            super.visitKtElement(element)
            if (element is KtLabelReferenceExpression) return

            val references = element.references.ifEmpty { return }
            val bindingContext = analyze(element) {
                (this as KtFe10AnalysisSession).analysisContext.analyze(element, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
            }
            val isResolved = hasResolvedDescriptor(element, bindingContext)

            for (reference in references) {
                if (reference !is KtReference) continue

                abstractRefs.add(AbstractReferenceImpl(reference))

                val names = reference.resolvesByNames.toSet()
                if (!isResolved) {
                    unresolvedNames += names
                }

                for (target in reference.targets(bindingContext)) {
                    val importableDescriptor = target.getImportableDescriptor()
                    val importableFqName = target.importableFqName ?: continue
                    val parentFqName = importableFqName.parent()
                    if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages

                    if (target !is PackageViewDescriptor && parentFqName == currentPackageName && (importableFqName !in aliases)) continue

                    if (!reference.canBeResolvedViaImport(target, bindingContext)) continue

                    if (importableDescriptor.name in names && isAccessibleAsMember(importableDescriptor, element, bindingContext)) {
                        continue
                    }

                    val descriptorNames = (aliases[importableFqName].orEmpty() + importableFqName.shortName()).intersect(names)
                    namesToImport.getOrPut(importableFqName) { hashSetOf() } += descriptorNames
                    descriptorsToImport += ImportableDescriptor(importableDescriptor, importableFqName)
                }
            }
        }

        private fun isAccessibleAsMember(target: DeclarationDescriptor, place: KtElement, bindingContext: BindingContext): Boolean {
            if (target.containingDeclaration !is ClassDescriptor) return false

            fun isInScope(scope: HierarchicalScope): Boolean {
                return when (target) {
                    is FunctionDescriptor ->
                        scope.findFunction(target.name, NoLookupLocation.FROM_IDE) { it == target } != null
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    is PropertyDescriptor ->
                        scope.findVariable(target.name, NoLookupLocation.FROM_IDE) { it == target } != null
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    is ClassDescriptor ->
                        scope.findClassifier(target.name, NoLookupLocation.FROM_IDE) == target
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    else -> false
                }
            }

            val resolutionScope = place.getResolutionScope(bindingContext) ?: return false
            val noImportsScope = resolutionScope.replaceImportingScopes(null)

            if (isInScope(noImportsScope)) return true
            // classes not accessible through receivers, only their constructors
            return if (target is ClassDescriptor) false
            else resolutionScope.getImplicitReceiversHierarchy().any { isInScope(it.type.memberScope.memberScopeAsImportingScope()) }
        }

        private class AbstractReferenceImpl(private val reference: KtReference) : AbstractReference {
            override val element: KtElement
                get() = reference.element

            override val dependsOnNames: Collection<Name>
                get() {
                    val resolvesByNames = reference.resolvesByNames
                    if (reference is KtInvokeFunctionReference) {
                        val additionalNames =
                            (reference.element.calleeExpression as? KtNameReferenceExpression)?.mainReference?.resolvesByNames

                        if (additionalNames != null) {
                            return resolvesByNames + additionalNames
                        }
                    }

                    return resolvesByNames
                }

            override fun toString() = when (reference) {
                is Fe10SyntheticPropertyAccessorReference -> {
                    reference.toString().replace(
                        "Fe10SyntheticPropertyAccessorReference",
                        if (reference.getter) "Getter" else "Setter"
                    )
                }

                else -> reference.toString().replace("Fe10", "")
            }
        }

        private fun KtReference.targets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
            //class qualifiers that refer to companion objects should be considered (containing) class references
            return bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element as? KtReferenceExpression]?.let { listOf(it) }
                ?: (this as? KtFe10Reference)?.resolveToDescriptors(bindingContext).orEmpty()
        }

        private fun hasResolvedDescriptor(element: KtElement, bindingContext: BindingContext): Boolean {
            return if (element is KtCallElement)
                element.getResolvedCall(bindingContext) != null
            else
                (element.mainReference as? KtFe10Reference)?.resolveToDescriptors(bindingContext)?.let { descriptors ->
                    descriptors.isNotEmpty() && descriptors.none { it is ErrorFunctionDescriptor }
                } == true
        }

        private val DeclarationDescriptor.importableFqName: FqName?
            get() {
                if (!canBeReferencedViaImport()) return null
                return getImportableDescriptor().fqNameSafe
            }

        private fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
            if (this is PackageViewDescriptor ||
                DescriptorUtils.isTopLevelDeclaration(this) ||
                this is CallableDescriptor && DescriptorUtils.isStaticDeclaration(this)
            ) {
                return !name.isSpecial
            }

            //Both TypeAliasDescriptor and ClassDescriptor
            val parentClassifier = containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: return false
            if (!parentClassifier.canBeReferencedViaImport()) return false

            return when (this) {
                is ConstructorDescriptor -> !parentClassifier.isInner // inner class constructors can't be referenced via import
                is ClassDescriptor, is TypeAliasDescriptor -> true
                else -> parentClassifier is ClassDescriptor && parentClassifier.kind == ClassKind.OBJECT
            }
        }

        private fun KtReference.canBeResolvedViaImport(target: DeclarationDescriptor, bindingContext: BindingContext): Boolean {
            if (this is KDocReference) {
                val qualifier = element.getQualifier() ?: return true
                return if (target.isExtension) {
                    val elementHasFunctionDescriptor =
                        element.resolveMainReferenceToDescriptors(bindingContext).any { it is FunctionDescriptor }
                    val qualifierHasClassDescriptor =
                        qualifier.resolveMainReferenceToDescriptors(bindingContext).any { it is ClassDescriptor }
                    elementHasFunctionDescriptor && qualifierHasClassDescriptor
                } else {
                    false
                }
            }
            return element.canBeResolvedViaImport(target, bindingContext)
        }

        fun KtElement.resolveMainReferenceToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
            return (mainReference as? KtFe10Reference)?.resolveToDescriptors(bindingContext).orEmpty()
        }


        private fun KtElement.canBeResolvedViaImport(target: DeclarationDescriptor, bindingContext: BindingContext): Boolean {
            if (!target.canBeReferencedViaImport()) return false
            if (target.isExtension) return true // assume that any type of reference can use imports when resolved to extension
            if (this !is KtNameReferenceExpression) return false

            val callTypeAndReceiver = CallTypeAndReceiver.detect(this)
            if (callTypeAndReceiver.receiver != null) {
                if (target !is PropertyDescriptor || !target.type.isExtensionFunctionType) return false
                if (callTypeAndReceiver !is CallTypeAndReceiver.DOT && callTypeAndReceiver !is CallTypeAndReceiver.SAFE) return false

                val resolvedCall = bindingContext[BindingContext.CALL, this].getResolvedCall(bindingContext)
                        as? VariableAsFunctionResolvedCall ?: return false
                if (resolvedCall.variableCall.explicitReceiverKind.isDispatchReceiver) return false
            }

            if (parent is KtThisExpression || parent is KtSuperExpression) return false
            return true
        }
    }

    private fun KtImportDirective.targetDescriptors(): Collection<DeclarationDescriptor> {
        // For codeFragments imports are created in dummy file
        //if (this.containingKtFile.doNotAnalyze != null) return emptyList()
        val nameExpression = importedReference?.getQualifiedElementSelector() as? KtSimpleNameExpression ?: return emptyList()
        val bindingContext = analyze(nameExpression) {
            (this as KtFe10AnalysisSession).analysisContext.analyze(nameExpression, Fe10AnalysisFacade.AnalysisMode.FULL)
        }
        return (nameExpression.mainReference as? KtFe10Reference)?.resolveToDescriptors(bindingContext).orEmpty()
    }
}