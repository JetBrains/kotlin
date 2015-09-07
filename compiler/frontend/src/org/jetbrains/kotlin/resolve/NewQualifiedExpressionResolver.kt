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

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.utils.addIfNotNull

public class NewQualifiedExpressionResolver(val symbolUsageValidator: SymbolUsageValidator) {


    public fun resolvePackageHeader(
            packageDirective: JetPackageDirective,
            module: ModuleDescriptor,
            trace: BindingTrace
    ) {
        for (nameExpression in packageDirective.packageNames) {
            storageResult(trace, nameExpression, listOf(module.getPackage(packageDirective.getFqName(nameExpression))), null)
        }
    }

    public fun processImportReference(
            importDirective: JetImportDirective,
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            shouldBeVisibleFrom: DeclarationDescriptor
    ): JetScope {
        val importedReference = importDirective.importedReference ?: return JetScope.Empty
        val path = importedReference.asQualifierPartList(trace)
        val lastPart = path.lastOrNull() ?: return JetScope.Empty

        if (importDirective.isAllUnder) {
            val packageOrClassDescriptor = resolveToPackageOrClass(path, moduleDescriptor, trace, shouldBeVisibleFrom) ?: return JetScope.Empty
            if (packageOrClassDescriptor is ClassDescriptor && packageOrClassDescriptor.kind.isSingleton) {
                trace.report(Errors.CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON.on(lastPart.expression, packageOrClassDescriptor))
            }
            val scope = AllUnderImportsScope()
            scope.addAllUnderImport(packageOrClassDescriptor)
            return scope
        }
        else {
            val packageOrClassDescriptor = resolveToPackageOrClass(path.subList(0, path.size() - 1), moduleDescriptor, trace, shouldBeVisibleFrom)
                                           ?: return JetScope.Empty
            val descriptors = SmartList<DeclarationDescriptor>()

            when (packageOrClassDescriptor) {
                is PackageViewDescriptor -> {
                    val packageScope = packageOrClassDescriptor.memberScope
                    descriptors.addIfNotNull(packageScope.getClassifier(lastPart.name, lastPart.location))
                    descriptors.addAll(packageScope.getProperties(lastPart.name, lastPart.location))
                    descriptors.addAll(packageScope.getFunctions(lastPart.name, lastPart.location))
                }
                is ClassDescriptor -> {
                    descriptors.addIfNotNull(
                            packageOrClassDescriptor.unsubstitutedInnerClassesScope.getClassifier(lastPart.name, lastPart.location)
                    )
                    val staticClassScope = packageOrClassDescriptor.staticScope
                    descriptors.addAll(staticClassScope.getFunctions(lastPart.name, lastPart.location))
                    descriptors.addAll(staticClassScope.getProperties(lastPart.name, lastPart.location))
                }
                // todo assert?
                else -> return JetScope.Empty
            }
            storageResult(trace, lastPart.expression, descriptors, shouldBeVisibleFrom)

            val aliasName = JetPsiUtil.getAliasName(importDirective) ?: return JetScope.Empty
            return SingleImportScope(aliasName, descriptors)
        }
    }

    private fun JetExpression.asQualifierPartList(trace: BindingTrace): List<QualifierPart> {
        val result = SmartList<QualifierPart>()
        var expression: JetExpression? = this
        loop@ while (expression != null) {
            when (expression) {
                is JetSimpleNameExpression -> {
                    result add QualifierPart(expression.getReferencedNameAsName(), expression)
                    break@loop
                }
                is JetQualifiedExpression -> {
                    (expression.selectorExpression as? JetSimpleNameExpression)?.let {
                        result add QualifierPart(it.getReferencedNameAsName(), it)
                    }
                    expression = expression.receiverExpression
                    if (expression is JetSafeQualifiedExpression) {
                        trace.report(Errors.SAFE_CALL_IN_QUALIFIER.on(expression.operationTokenNode.psi))
                    }
                }
                else -> expression = null
            }
        }
        return result.asReversed()
    }

    private data class QualifierPart(
            val name: Name,
            val expression: JetSimpleNameExpression,
            val typeArguments: JetTypeArgumentList? = null
    ) {
        val location = KotlinLookupLocation(expression)
    }

    private fun resolveToPackageOrClass(
            path: List<QualifierPart>,
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            shouldBeVisibleFrom: DeclarationDescriptor,
            firstPartResolver: (QualifierPart) -> DeclarationDescriptor? = { null }
    ): DeclarationDescriptor? {
        if (path.isEmpty()) {
            return moduleDescriptor.getPackage(FqName.ROOT)
        }

        val (currentDescriptor, currentIndex) = firstPartResolver(path.first())?.let {
            Pair(it, 1)
        } ?: moduleDescriptor.quickResolveToPackage(path, trace)
        return path.subList(currentIndex, path.size()).fold<QualifierPart, DeclarationDescriptor?>(currentDescriptor) {
            descriptor, qualifierPart ->
            val nextDescriptor = when (descriptor) {
                // TODO: support inner classes which captured type parameter from outer class
                is ClassDescriptor ->
                    descriptor.unsubstitutedInnerClassesScope.getClassifier(qualifierPart.name, qualifierPart.location)
                is PackageViewDescriptor -> {
                    val packageView = if (qualifierPart.typeArguments == null) {
                        moduleDescriptor.getPackage(descriptor.fqName.child(qualifierPart.name))
                    } else null


                    if (packageView != null && !packageView.isEmpty()) {
                        packageView
                    } else {
                        descriptor.memberScope.getClassifier(qualifierPart.name, qualifierPart.location)
                    }
                }
                else -> null
            }
            storageResult(trace, qualifierPart.expression, listOfNotNull(nextDescriptor), shouldBeVisibleFrom)
            nextDescriptor
        }
    }

    private fun ModuleDescriptor.quickResolveToPackage(
            path: List<QualifierPart>,
            trace: BindingTrace
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize = path.indexOfFirst { it.typeArguments != null }.let { if (it == -1) path.size() else it + 1 }
        var fqName = path.subList(0, possiblePackagePrefixSize).fold(FqName.ROOT) { fqName, qualifierPart ->
            fqName.child(qualifierPart.name)
        }
        var prefixSize = possiblePackagePrefixSize
        while (!fqName.isRoot) {
            val packageDescriptor = getPackage(fqName)
            if (!packageDescriptor.isEmpty()) {
                recordPackageViews(path.subList(0, prefixSize), packageDescriptor, trace)
                return Pair(packageDescriptor, prefixSize)
            }
            fqName = fqName.parent()
            prefixSize--
        }
        return Pair(getPackage(FqName.ROOT), 0)
    }

    private fun recordPackageViews(
            path: List<QualifierPart>,
            packageView: PackageViewDescriptor,
            trace: BindingTrace
    ) {
        path.foldRight(packageView) { qualifierPart, currentView ->
            storageResult(trace, qualifierPart.expression, listOfNotNull(currentView), null)
            val parentView = currentView.containingDeclaration
            assert(parentView != null) {
                "Containing Declaration must be not null for package with fqName: ${currentView.fqName}, " +
                "path: ${path.joinToString()}, packageView fqName: ${packageView.fqName}"
            }
            parentView!!
        }
    }

    private fun storageResult(
            trace: BindingTrace,
            referenceExpression: JetSimpleNameExpression,
            descriptors: Collection<DeclarationDescriptor>,
            shouldBeVisibleFrom: DeclarationDescriptor?
    ) {
        if (descriptors.isEmpty()) {
            trace.report(Errors.UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
        }
        else if(descriptors.size() > 1) {
            // todo all descriptors invisible - report specific error
            trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, descriptors)
        }
        else {
            val descriptor = descriptors.single()
            trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptor)
            if (descriptor is ClassifierDescriptor) {
                symbolUsageValidator.validateTypeUsage(descriptor, trace, referenceExpression)
            }
            if (descriptor is DeclarationDescriptorWithVisibility && shouldBeVisibleFrom != null) {
                checkVisibility(descriptor, trace, referenceExpression, shouldBeVisibleFrom)
            }

            storageQualifier(trace, referenceExpression, descriptor)
        }
    }

    private fun storageQualifier(trace: BindingTrace, referenceExpression: JetSimpleNameExpression, descriptor: DeclarationDescriptor) {
        if (descriptor is PackageViewDescriptor || descriptor is ClassifierDescriptor) {
            val qualifier = QualifierReceiver(referenceExpression, descriptor as? PackageViewDescriptor, descriptor as? ClassifierDescriptor)
            trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)
        }
    }

    private fun checkVisibility(
            descriptor: DeclarationDescriptorWithVisibility,
            trace: BindingTrace,
            referenceExpression: JetSimpleNameExpression,
            shouldBeVisibleFrom: DeclarationDescriptor) {
        val visibility = descriptor.visibility
        if (PsiTreeUtil.getParentOfType(referenceExpression, JetImportDirective::class.java) != null && !visibility.mustCheckInImports()) {
            return
        }
        if (!Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, descriptor, shouldBeVisibleFrom)) {
            trace.report(Errors.INVISIBLE_REFERENCE.on(referenceExpression, descriptor, visibility, descriptor.containingDeclaration!!))
        }
    }
}