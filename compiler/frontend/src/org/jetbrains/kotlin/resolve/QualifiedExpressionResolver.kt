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
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.getClassifier
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.utils.addIfNotNull

public class QualifiedExpressionResolver(val symbolUsageValidator: SymbolUsageValidator) {


    public fun resolvePackageHeader(
            packageDirective: JetPackageDirective,
            module: ModuleDescriptor,
            trace: BindingTrace
    ) {
        for (nameExpression in packageDirective.packageNames) {
            storageResult(trace, nameExpression, listOf(module.getPackage(packageDirective.getFqName(nameExpression))), null)
        }
    }

    public fun resolveDescriptorForUserType(
            userType: JetUserType,
            scope: LexicalScope,
            trace: BindingTrace
    ): ClassifierDescriptor? {
        if (userType.qualifier == null && !userType.startWithPackage) { // optimization for non-qualified types
            return userType.referenceExpression?.let {
                val classifier = scope.getClassifier(it.getReferencedNameAsName(), KotlinLookupLocation(it))
                storageResult(trace, it, listOfNotNull(classifier), scope.ownerDescriptor)
                classifier
            }
        }

        val module = scope.ownerDescriptor.module
        val (qualifierPartList, hasError) = userType.asQualifierPartList()
        if (hasError) {
            resolveToPackageOrClass(qualifierPartList, module, trace, scope.ownerDescriptor)
            return null
        }
        assert(qualifierPartList.size() >= 1) {
            "Too short qualifier list for user type $userType : ${qualifierPartList.joinToString()}"
        }

        val qualifier = resolveToPackageOrClass(
                qualifierPartList.subList(0, qualifierPartList.size() - 1), module, trace, scope.ownerDescriptor,
                firstPartResolver =  {
                    if (userType.startWithPackage) {
                        null
                    }
                    else {
                        scope.getClassifier(it.name, it.location)
                    }
                }
        ) ?: return null

        val lastPart = qualifierPartList.last()
        val classifier = when (qualifier) {
            is PackageViewDescriptor -> qualifier.memberScope.getClassifier(lastPart.name, lastPart.location)
            is ClassDescriptor -> qualifier.unsubstitutedInnerClassesScope.getClassifier(lastPart.name, lastPart.location)
            else -> null
        }
        storageResult(trace, lastPart.expression, listOfNotNull(classifier), scope.ownerDescriptor)
        return classifier
    }

    private val JetUserType.startWithPackage: Boolean
        get() {
            var firstPart = this
            while (firstPart.qualifier != null) {
                firstPart = firstPart.qualifier!!
            }
            return firstPart.isAbsoluteInRootPackage
        }


    private fun JetUserType.asQualifierPartList(): Pair<List<QualifierPart>, Boolean> {
        var hasError = false
        val result = SmartList<QualifierPart>()
        var userType: JetUserType? = this
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
            importDirective: JetImportDirective,
            moduleDescriptor: ModuleDescriptor,
            trace: BindingTrace,
            shouldBeVisibleFrom: DeclarationDescriptor // todo
    ): JetScope {
        val importedReference = importDirective.importedReference ?: return JetScope.Empty
        val path = importedReference.asQualifierPartList(trace)
        val lastPart = path.lastOrNull() ?: return JetScope.Empty

        if (importDirective.isAllUnder) {
            val packageOrClassDescriptor = resolveToPackageOrClass(path, moduleDescriptor, trace, shouldBeVisibleFrom) ?: return JetScope.Empty
            if (packageOrClassDescriptor is ClassDescriptor && packageOrClassDescriptor.kind.isSingleton) {
                trace.report(Errors.CANNOT_IMPORT_MEMBERS_FROM_SINGLETON.on(lastPart.expression, packageOrClassDescriptor)) // todo report on star
            }
            val scope = AllUnderImportsScope()
            scope.addAllUnderImport(packageOrClassDescriptor)
            return scope
        }
        else {
            val aliasName = JetPsiUtil.getAliasName(importDirective)
            if (aliasName == null) { // import kotlin.
                resolveToPackageOrClass(path, moduleDescriptor, trace, shouldBeVisibleFrom)
                return JetScope.Empty
            }

            val packageOrClassDescriptor = resolveToPackageOrClass(path.subList(0, path.size() - 1), moduleDescriptor, trace, shouldBeVisibleFrom)
                                           ?: return JetScope.Empty
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
                }

                else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
            }
            if (descriptors.isNotEmpty()) {
                storageResult(trace, lastPart.expression, descriptors, shouldBeVisibleFrom)
            }
            else {
                tryResolveDescriptorsWhichCannotBeImported(trace, moduleDescriptor, packageOrClassDescriptor, lastPart)
            }

            return SingleImportScope(aliasName, descriptors)
        }
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
                    if (packageOrClassDescriptor.kind.isSingleton) {
                        trace.report(Errors.CANNOT_IMPORT_MEMBERS_FROM_SINGLETON.on(lastPart.expression, packageOrClassDescriptor))
                    }
                    else {
                        trace.report(Errors.CANNOT_BE_IMPORTED.on(lastPart.expression, lastName))
                    }
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
        storageResult(trace, lastPart.expression, descriptors, shouldBeVisibleFrom = null)
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
            storageResult(trace, path.first().expression, listOf(it), shouldBeVisibleFrom)
            Pair(it, 1)
        } ?: moduleDescriptor.quickResolveToPackage(path, trace)
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