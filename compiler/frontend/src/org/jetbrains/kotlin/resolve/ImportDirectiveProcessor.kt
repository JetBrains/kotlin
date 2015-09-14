/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.JetScope
import java.util.ArrayList

public class ImportDirectiveProcessor(
        private val qualifiedExpressionResolver: QualifiedExpressionResolver
) {
    public fun processImportReference(
            importDirective: JetImportDirective,
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            lookupMode: QualifiedExpressionResolver.LookupMode
    ): JetScope {
        if (importDirective.isAbsoluteInRootPackage()) {
            trace.report(Errors.UNSUPPORTED.on(importDirective, "TypeHierarchyResolver")) // TODO
            return JetScope.Empty
        }

        val importedReference = importDirective.getImportedReference() ?: return JetScope.Empty
        val importPath = importDirective.getImportPath() ?: return JetScope.Empty
        val (packageViewDescriptor, selectorsToLookUp) = tryResolvePackagesFromRightToLeft(
                moduleDescriptor, trace, importPath.fqnPart(), importedReference
        )
        val descriptors = lookUpMembersFromLeftToRight(moduleDescriptor, trace, packageViewDescriptor, selectorsToLookUp, lookupMode)

        val referenceExpression = JetPsiUtil.getLastReference(importedReference)
        if (importDirective.isAllUnder()) {
            if (!canAllUnderImportFrom(descriptors) && referenceExpression != null) {
                val toReportOn = descriptors.filterIsInstance<ClassDescriptor>().first()
                trace.report(Errors.CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON.on(referenceExpression, toReportOn))
            }

            if (referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression, trace, lookupMode)) {
                return JetScope.Empty
            }

            val importsScope = AllUnderImportsScope()
            for (descriptor in descriptors) {
                importsScope.addAllUnderImport(descriptor)
            }
            return importsScope
        }
        else {
            val aliasName = JetPsiUtil.getAliasName(importDirective) ?: return JetScope.Empty
            return SingleImportScope(aliasName, descriptors)
        }
    }

    private fun tryResolvePackagesFromRightToLeft(
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            fqName: FqName,
            jetExpression: JetExpression?
    ): Pair<PackageViewDescriptor, List<JetSimpleNameExpression>> {
        val selectorsToLookUp = ArrayList<JetSimpleNameExpression>()

        fun recTryResolvePackagesFromRightToLeft(
                fqName: FqName,
                jetExpression: JetExpression?
        ): PackageViewDescriptor {
            val packageView = moduleDescriptor.getPackage(fqName)
            if (jetExpression == null) {
                assert(fqName.isRoot())
                return packageView
            }
            return when {
                !packageView.isEmpty() -> {
                    recordPackageViews(jetExpression, packageView, trace)
                    packageView
                }
                else -> {
                    assert(!fqName.isRoot())
                    val (expressionRest, selector) = jetExpression.getReceiverAndSelector()
                    if (selector != null) {
                        selectorsToLookUp.add(selector)
                    }
                    recTryResolvePackagesFromRightToLeft(fqName.parent(), expressionRest)
                }
            }
        }

        val packageView = recTryResolvePackagesFromRightToLeft(fqName, jetExpression)
        return Pair(packageView, selectorsToLookUp.reverse())
    }

    private fun recordPackageViews(jetExpression: JetExpression, packageView: PackageViewDescriptor, trace: BindingTrace) {
        trace.record(BindingContext.REFERENCE_TARGET, JetPsiUtil.getLastReference(jetExpression), packageView)
        val containingView = packageView.getContainingDeclaration()
        val (receiver, _) = jetExpression.getReceiverAndSelector()
        if (containingView != null && receiver != null) {
            recordPackageViews(receiver, containingView, trace)
        }
    }

    private fun lookUpMembersFromLeftToRight(
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            packageView: PackageViewDescriptor,
            selectorsToLookUp: List<JetSimpleNameExpression>,
            lookupMode: QualifiedExpressionResolver.LookupMode
    ): Collection<DeclarationDescriptor> {
        var currentDescriptors: Collection<DeclarationDescriptor> = listOf(packageView)
        for ((i, selector) in selectorsToLookUp.withIndex()) {
            currentDescriptors = qualifiedExpressionResolver.lookupSelectorDescriptors(
                    selector, currentDescriptors, trace, moduleDescriptor, lookupMode, lookupMode.isEverything()
            )
            val isLastReference = i == selectorsToLookUp.size() - 1
            if (!isLastReference && !canImportMembersFrom(currentDescriptors, selector, trace, lookupMode)) {
                return emptyList()
            }
        }
        return currentDescriptors
    }

    private fun JetExpression.getReceiverAndSelector(): Pair<JetExpression?, JetSimpleNameExpression?> {
        when (this) {
            is JetDotQualifiedExpression -> {
                return Pair(this.getReceiverExpression(), this.getSelectorExpression() as? JetSimpleNameExpression)
            }
            is JetSimpleNameExpression -> {
                return Pair(null, this)
            }
            else -> {
                throw AssertionError("Invalid expression in import $this of class ${this.javaClass}")
            }
        }
    }

    public companion object {
        public fun canAllUnderImportFrom(descriptors: Collection<DeclarationDescriptor>): Boolean {
            if (descriptors.isEmpty()) {
                return true
            }
            return descriptors.any { it !is ClassDescriptor || canAllUnderImportFromClass(it) }
        }

        public fun canAllUnderImportFromClass(descriptor: ClassDescriptor): Boolean = !descriptor.getKind().isSingleton()

        @JvmStatic
        public fun canImportMembersFrom(
                descriptors: Collection<DeclarationDescriptor>,
                reference: JetSimpleNameExpression,
                trace: BindingTrace,
                lookupMode: QualifiedExpressionResolver.LookupMode
        ): Boolean {
            if (lookupMode.isOnlyClassesAndPackages()) {
                return true
            }

            descriptors.singleOrNull()?.let { return canImportMembersFrom(it, reference, trace, lookupMode) }

            val temporaryTrace = TemporaryBindingTrace.create(trace, "trace to find out if members can be imported from", reference)
            var canImport = false
            for (descriptor in descriptors) {
                canImport = canImport || canImportMembersFrom(descriptor, reference, temporaryTrace, lookupMode)
            }
            if (!canImport) {
                temporaryTrace.commit()
            }
            return canImport
        }

        private fun canImportMembersFrom(
                descriptor: DeclarationDescriptor,
                reference: JetSimpleNameExpression,
                trace: BindingTrace,
                lookupMode: QualifiedExpressionResolver.LookupMode
        ): Boolean {
            assert(lookupMode.isEverything())
            if (descriptor is PackageViewDescriptor || descriptor is ClassDescriptor) {
                return true
            }
            trace.report(Errors.CANNOT_IMPORT_FROM_ELEMENT.on(reference, descriptor))
            return false
        }
    }
}

private fun QualifiedExpressionResolver.LookupMode.isEverything() = this == QualifiedExpressionResolver.LookupMode.EVERYTHING
private fun QualifiedExpressionResolver.LookupMode.isOnlyClassesAndPackages() = this == QualifiedExpressionResolver.LookupMode.ONLY_CLASSES_AND_PACKAGES