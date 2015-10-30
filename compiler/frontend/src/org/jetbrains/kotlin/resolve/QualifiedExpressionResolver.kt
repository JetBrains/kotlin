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

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.check

public class QualifiedExpressionResolver(val symbolUsageValidator: SymbolUsageValidator) {


    public fun resolvePackageHeader(
            packageDirective: KtPackageDirective,
            module: ModuleDescriptor,
            trace: BindingTrace
    ) {
        val packageNames = packageDirective.packageNames
        for ((index, nameExpression) in packageNames.withIndex()) {
            storeResult(trace, nameExpression, module.getPackage(packageDirective.getFqName(nameExpression)),
                        shouldBeVisibleFrom = null, inImport = false, isQualifier = index != packageNames.lastIndex)
        }
    }

    public fun resolveDescriptorForUserType(
            userType: KtUserType,
            scope: LexicalScope,
            trace: BindingTrace
    ): ClassifierDescriptor? {
        if (userType.qualifier == null && !userType.startWithPackage) { // optimization for non-qualified types
            return userType.referenceExpression?.let {
                val classifier = scope.findClassifier(it.getReferencedNameAsName(), KotlinLookupLocation(it))
                storeResult(trace, it, classifier, scope.ownerDescriptor, inImport = false, isQualifier = false)
                classifier
            }
        }

        val module = scope.ownerDescriptor.module
        val (qualifierPartList, hasError) = userType.asQualifierPartList()
        if (hasError) {
            resolveToPackageOrClass(qualifierPartList, module, trace, scope.ownerDescriptor, scope, inImport = false)
            return null
        }
        assert(qualifierPartList.size() >= 1) {
            "Too short qualifier list for user type $userType : ${qualifierPartList.joinToString()}"
        }

        val qualifier = resolveToPackageOrClass(
                qualifierPartList.subList(0, qualifierPartList.size() - 1), module,
                trace, scope.ownerDescriptor, scope.check { !userType.startWithPackage }, inImport = false
        ) ?: return null

        val lastPart = qualifierPartList.last()
        val classifier = when (qualifier) {
            is PackageViewDescriptor -> qualifier.memberScope.getClassifier(lastPart.name, lastPart.location)
            is ClassDescriptor -> qualifier.unsubstitutedInnerClassesScope.getClassifier(lastPart.name, lastPart.location)
            else -> null
        }
        storeResult(trace, lastPart.expression, classifier, scope.ownerDescriptor, inImport = false, isQualifier = false)
        return classifier
    }

    private val KtUserType.startWithPackage: Boolean
        get() {
            var firstPart = this
            while (firstPart.qualifier != null) {
                firstPart = firstPart.qualifier!!
            }
            return firstPart.isAbsoluteInRootPackage
        }


    private fun KtUserType.asQualifierPartList(): Pair<List<QualifierPart>, Boolean> {
        var hasError = false
        val result = SmartList<QualifierPart>()
        var userType: KtUserType? = this
        while (userType != null) {
            val referenceExpression = userType.referenceExpression
            if (referenceExpression != null) {
                result add QualifierPart(referenceExpression.getReferencedNameAsName(), referenceExpression, userType.typeArgumentList)
            }
            else {
                hasError = true
            }
            userType = userType.qualifier
        }
        return result.asReversed() to hasError
    }

    public fun processImportReference(
            importDirective: KtImportDirective,
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? { // null if some error happened
        val importedReference = importDirective.importedReference ?: return null
        val path = importedReference.asQualifierPartList(trace)
        val lastPart = path.lastOrNull() ?: return null

        if (!importDirective.isAllUnder) {
            return processSingleImport(moduleDescriptor, trace, importDirective, path, lastPart, packageFragmentForVisibilityCheck)
        }
        val packageOrClassDescriptor = resolveToPackageOrClass(path, moduleDescriptor, trace, packageFragmentForVisibilityCheck,
                                                               scopeForFirstPart = null, inImport = true) ?: return null
        if (packageOrClassDescriptor is ClassDescriptor && packageOrClassDescriptor.kind.isSingleton) {
            trace.report(Errors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON.on(lastPart.expression, packageOrClassDescriptor)) // todo report on star
        }
        return AllUnderImportsScope(packageOrClassDescriptor)
}

    private fun processSingleImport(
            moduleDescriptor: ModuleDescriptor, trace: BindingTrace, importDirective: KtImportDirective,
            path: List<QualifierPart>, lastPart: QualifierPart, packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): SingleImportScope? {
        val aliasName = KtPsiUtil.getAliasName(importDirective)
        if (aliasName == null) {
            // import kotlin.
            resolveToPackageOrClass(path, moduleDescriptor, trace, packageFragmentForVisibilityCheck, scopeForFirstPart = null, inImport = true)
            return null
        }

        val packageOrClassDescriptor = resolveToPackageOrClass(
                path.subList(0, path.size() - 1), moduleDescriptor, trace,
                packageFragmentForVisibilityCheck, scopeForFirstPart = null, inImport = true
        ) ?: return null

        val candidates = collectCandidateDescriptors(lastPart, packageOrClassDescriptor)
        if (candidates.isNotEmpty()) {
            storeResult(trace, lastPart.expression, candidates, packageFragmentForVisibilityCheck, inImport = true, isQualifier = false)
        }
        else {
            tryResolveDescriptorsWhichCannotBeImported(trace, moduleDescriptor, packageOrClassDescriptor, lastPart)
            return null
        }

        val importedDescriptors = candidates.filter { isVisible(it, packageFragmentForVisibilityCheck, inImport = true) }.
                check { it.isNotEmpty() } ?: candidates

        return SingleImportScope(aliasName, importedDescriptors)
    }

    private fun collectCandidateDescriptors(lastPart: QualifierPart, packageOrClassDescriptor: DeclarationDescriptor): SmartList<DeclarationDescriptor> {
        val descriptors = SmartList<DeclarationDescriptor>()

        val lastName = lastPart.name
        val location = lastPart.location
        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageScope = packageOrClassDescriptor.memberScope
                descriptors.addIfNotNull(packageScope.getClassifier(lastName, location))
                descriptors.addAll(packageScope.getProperties(lastName, location))
                descriptors.addAll(packageScope.getFunctions(lastName, location))
            }

            is ClassDescriptor -> {
                descriptors.addIfNotNull(
                        packageOrClassDescriptor.unsubstitutedInnerClassesScope.getClassifier(lastName, location)
                )
                val staticClassScope = packageOrClassDescriptor.staticScope
                descriptors.addAll(staticClassScope.getFunctions(lastName, location))
                descriptors.addAll(staticClassScope.getProperties(lastName, location))

                if (packageOrClassDescriptor.kind == ClassKind.OBJECT) {
                    descriptors.addAll(packageOrClassDescriptor.unsubstitutedMemberScope.getFunctions(lastName, location).map {
                        FunctionImportedFromObject(it)
                    })
                    val properties = packageOrClassDescriptor.unsubstitutedMemberScope.getProperties(lastName, location)
                            .filterIsInstance<PropertyDescriptor>().map { PropertyImportedFromObject(it) }
                    descriptors.addAll(properties)
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
        return descriptors
    }

    private fun tryResolveDescriptorsWhichCannotBeImported(
            trace: BindingTrace,
            moduleDescriptor: ModuleDescriptor,
            packageOrClassDescriptor: DeclarationDescriptor,
            lastPart: QualifierPart
    ) {
        val descriptors = SmartList<DeclarationDescriptor>()
        val lastName = lastPart.name
        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageDescriptor = moduleDescriptor.getPackage(packageOrClassDescriptor.fqName.child(lastName))
                if (!packageDescriptor.isEmpty()) {
                    trace.report(Errors.PACKAGE_CANNOT_BE_IMPORTED.on(lastPart.expression))
                    descriptors.add(packageOrClassDescriptor)
                }
            }

            is ClassDescriptor -> {
                val memberScope = packageOrClassDescriptor.unsubstitutedMemberScope
                descriptors.addAll(memberScope.getFunctions(lastName, lastPart.location))
                descriptors.addAll(memberScope.getProperties(lastName, lastPart.location))
                if (descriptors.isNotEmpty()) {
                    trace.report(Errors.CANNOT_BE_IMPORTED.on(lastPart.expression, lastName))
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
        storeResult(trace, lastPart.expression, descriptors, shouldBeVisibleFrom = null, inImport = true, isQualifier = false)
    }

    private fun KtExpression.asQualifierPartList(trace: BindingTrace): List<QualifierPart> {
        val result = SmartList<QualifierPart>()
        var expression: KtExpression? = this
        loop@ while (expression != null) {
            when (expression) {
                is KtSimpleNameExpression -> {
                    result add QualifierPart(expression.getReferencedNameAsName(), expression)
                    break@loop
                }
                is KtQualifiedExpression -> {
                    (expression.selectorExpression as? KtSimpleNameExpression)?.let {
                        result add QualifierPart(it.getReferencedNameAsName(), it)
                    }
                    expression = expression.receiverExpression
                    if (expression is KtSafeQualifiedExpression) {
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
            val expression: KtSimpleNameExpression,
            val typeArguments: KtTypeArgumentList? = null
    ) {
        val location = KotlinLookupLocation(expression)
    }

    private fun resolveToPackageOrClass(
            path: List<QualifierPart>,
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            shouldBeVisibleFrom: DeclarationDescriptor?,
            scopeForFirstPart: LexicalScope?,
            inImport: Boolean
    ): DeclarationDescriptor? {
        if (path.isEmpty()) {
            return moduleDescriptor.getPackage(FqName.ROOT)
        }

        val firstDescriptor = scopeForFirstPart?.let {
                val firstPart = path.first()
                it.findClassifier(firstPart.name, firstPart.location)?.apply {
                    storeResult(trace, firstPart.expression, this, shouldBeVisibleFrom, inImport)
                }
            }


        val (currentDescriptor, currentIndex) = firstDescriptor?.let { Pair(it, 1) } ?: moduleDescriptor.quickResolveToPackage(path, trace, inImport)

        return path.subList(currentIndex, path.size()).fold<QualifierPart, DeclarationDescriptor?>(currentDescriptor) {
            descriptor, qualifierPart ->
            // report unresolved reference only for first unresolved qualifier
            if (descriptor == null) return@fold null

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
            storeResult(trace, qualifierPart.expression, nextDescriptor, shouldBeVisibleFrom, inImport)
            nextDescriptor
        }
    }

    private fun ModuleDescriptor.quickResolveToPackage(
            path: List<QualifierPart>,
            trace: BindingTrace,
            inImport: Boolean
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize = path.indexOfFirst { it.typeArguments != null }.let { if (it == -1) path.size() else it + 1 }
        var fqName = path.subList(0, possiblePackagePrefixSize).fold(FqName.ROOT) { fqName, qualifierPart ->
            fqName.child(qualifierPart.name)
        }
        var prefixSize = possiblePackagePrefixSize
        while (!fqName.isRoot) {
            val packageDescriptor = getPackage(fqName)
            if (!packageDescriptor.isEmpty()) {
                recordPackageViews(path.subList(0, prefixSize), packageDescriptor, trace, inImport)
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
            trace: BindingTrace,
            inImport: Boolean
    ) {
        path.foldRight(packageView) { qualifierPart, currentView ->
            storeResult(trace, qualifierPart.expression, currentView, shouldBeVisibleFrom = null, inImport = inImport)
            val parentView = currentView.containingDeclaration
            assert(parentView != null) {
                "Containing Declaration must be not null for package with fqName: ${currentView.fqName}, " +
                "path: ${path.joinToString()}, packageView fqName: ${packageView.fqName}"
            }
            parentView!!
        }
    }

    private fun storeResult(
            trace: BindingTrace,
            referenceExpression: KtSimpleNameExpression,
            descriptors: Collection<DeclarationDescriptor>,
            shouldBeVisibleFrom: DeclarationDescriptor?,
            inImport: Boolean,
            isQualifier: Boolean = true
    ) {
        if (descriptors.size() > 1) {
            val visibleDescriptors = descriptors.filter { isVisible(it, shouldBeVisibleFrom, inImport) }
            if (visibleDescriptors.isEmpty()) {
                val descriptor = descriptors.first() as DeclarationDescriptorWithVisibility
                trace.report(Errors.INVISIBLE_REFERENCE.on(referenceExpression, descriptor, descriptor.visibility, descriptor))
            }
            else if (visibleDescriptors.size() > 1) {
                trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, visibleDescriptors)
            }
            else {
                storeResult(trace, referenceExpression, visibleDescriptors.single(), null, inImport, isQualifier)
            }
        }
        else {
            storeResult(trace, referenceExpression, descriptors.singleOrNull(), shouldBeVisibleFrom, inImport, isQualifier)
        }
    }

    private fun storeResult(
            trace: BindingTrace,
            referenceExpression: KtSimpleNameExpression,
            descriptor: DeclarationDescriptor?,
            shouldBeVisibleFrom: DeclarationDescriptor?,
            inImport: Boolean,
            isQualifier: Boolean = true
    ) {
        if (descriptor == null) {
            trace.report(Errors.UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
            return
        }

        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptor)

        if (descriptor is ClassifierDescriptor) {
            symbolUsageValidator.validateTypeUsage(descriptor, trace, referenceExpression)
        }

        if (descriptor is DeclarationDescriptorWithVisibility && !isVisible(descriptor, shouldBeVisibleFrom, inImport)) {
            trace.report(Errors.INVISIBLE_REFERENCE.on(referenceExpression, descriptor, descriptor.visibility, descriptor))
        }

        if (isQualifier) {
            storageQualifier(trace, referenceExpression, descriptor)
        }
    }

    private fun storageQualifier(trace: BindingTrace, referenceExpression: KtSimpleNameExpression, descriptor: DeclarationDescriptor) {
        if (descriptor is PackageViewDescriptor || descriptor is ClassifierDescriptor) {
            val qualifier = QualifierReceiver(referenceExpression, descriptor as? PackageViewDescriptor, descriptor as? ClassifierDescriptor)
            trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)
        }
    }

    private fun isVisible(
            descriptor: DeclarationDescriptor,
            shouldBeVisibleFrom: DeclarationDescriptor?,
            inImport: Boolean
    ): Boolean {
        if (descriptor !is DeclarationDescriptorWithVisibility || shouldBeVisibleFrom == null) return true

        val visibility = descriptor.visibility
        if (inImport) {
            if (Visibilities.isPrivate(visibility)) return false
            if (!visibility.mustCheckInImports()) return true
        }
        return Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, descriptor, shouldBeVisibleFrom)
    }
}
