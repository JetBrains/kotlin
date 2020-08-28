/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.psi.impl.source.DummyHolder
import com.intellij.util.SmartList
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.calls.CallExpressionElement
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.unrollToLeftMostQualifiedExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.scopes.utils.canBeResolvedWithoutDeprecation
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstClassifierWithDeprecationStatus
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.isWithoutValueArguments
import org.jetbrains.kotlin.utils.CallOnceFunction

class QualifiedExpressionResolver(val languageVersionSettings: LanguageVersionSettings) {
    fun resolvePackageHeader(
        packageDirective: KtPackageDirective,
        module: ModuleDescriptor,
        trace: BindingTrace
    ) {
        val packageNames = packageDirective.packageNames
        for ((index, nameExpression) in packageNames.withIndex()) {
            storeResult(
                trace, nameExpression, module.getPackage(packageDirective.getFqName(nameExpression)),
                shouldBeVisibleFrom = null, position = QualifierPosition.PACKAGE_HEADER, isQualifier = index != packageNames.lastIndex
            )
        }
    }

    data class TypeQualifierResolutionResult(
        val qualifierParts: List<ExpressionQualifierPart>,
        val classifierDescriptor: ClassifierDescriptor? = null
    ) {
        val allProjections: List<KtTypeProjection>
            get() = qualifierParts.flatMap { it.typeArguments?.arguments.orEmpty() }
    }

    fun LexicalScope.findClassifierAndReportDeprecationIfNeeded(
        name: Name,
        lookupLocation: KotlinLookupLocation,
        reportOn: KtExpression?,
        trace: BindingTrace
    ): ClassifierDescriptor? {
        val (classifier, isDeprecated) = findFirstClassifierWithDeprecationStatus(name, lookupLocation) ?: return null

        if (isDeprecated && reportOn != null) {
            trace.record(BindingContext.DEPRECATED_SHORT_NAME_ACCESS, reportOn) // For IDE

            // slow-path: we know that closest classifier is imported by the deprecated path, but before reporting
            // deprecation, we have to recheck if there's some other import path, which isn't deprecated (e.g. explicit import)
            if (!classifier.canBeResolvedWithoutDeprecation(this, lookupLocation)) {
                trace.report(Errors.DEPRECATED_ACCESS_BY_SHORT_NAME.on(reportOn, classifier))
            }
        }

        return classifier
    }

    fun resolveDescriptorForType(
        userType: KtUserType,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val ownerDescriptor = if (!isDebuggerContext) scope.ownerDescriptor else null
        if (userType.qualifier == null) {
            val descriptor = userType.referenceExpression?.let { expression ->
                val classifier = scope.findClassifierAndReportDeprecationIfNeeded(
                    expression.getReferencedNameAsName(),
                    KotlinLookupLocation(expression),
                    expression,
                    trace
                )

                checkNotEnumEntry(classifier, trace, expression)
                storeResult(trace, expression, classifier, ownerDescriptor, position = QualifierPosition.TYPE, isQualifier = false)
                classifier
            }

            return TypeQualifierResolutionResult(userType.asQualifierPartList().first, descriptor)
        }

        val (qualifierPartList, hasError) = userType.asQualifierPartList()
        if (hasError) {
            val descriptor = resolveToPackageOrClass(
                qualifierPartList, scope.ownerDescriptor.module, trace, ownerDescriptor, scope, position = QualifierPosition.TYPE
            ) as? ClassifierDescriptor
            return TypeQualifierResolutionResult(qualifierPartList, descriptor)
        }

        return resolveQualifierPartListForType(qualifierPartList, ownerDescriptor, scope, trace, isQualifier = false)
    }

    private fun resolveQualifierPartListForType(
        qualifierPartList: List<ExpressionQualifierPart>,
        ownerDescriptor: DeclarationDescriptor?,
        scope: LexicalScope,
        trace: BindingTrace,
        isQualifier: Boolean
    ): TypeQualifierResolutionResult {
        assert(qualifierPartList.isNotEmpty()) { "Qualifier list should not be empty" }

        val qualifier = resolveToPackageOrClass(
            qualifierPartList.subList(0, qualifierPartList.size - 1),
            scope.ownerDescriptor.module, trace, ownerDescriptor, scope,
            position = QualifierPosition.TYPE
        ) ?: return TypeQualifierResolutionResult(qualifierPartList, null)

        val lastPart = qualifierPartList.last()
        val classifier = when (qualifier) {
            is PackageViewDescriptor -> qualifier.memberScope.getContributedClassifier(lastPart.name, lastPart.location)
            is ClassDescriptor -> {
                val descriptor = qualifier.unsubstitutedInnerClassesScope.getContributedClassifier(lastPart.name, lastPart.location)
                checkNotEnumEntry(descriptor, trace, lastPart.expression)
                descriptor
            }
            else -> null
        }
        storeResult(trace, lastPart.expression, classifier, ownerDescriptor, position = QualifierPosition.TYPE, isQualifier = isQualifier)
        return TypeQualifierResolutionResult(qualifierPartList, classifier)
    }

    private fun checkNotEnumEntry(descriptor: DeclarationDescriptor?, trace: BindingTrace, expression: KtSimpleNameExpression?) {
        expression ?: return
        if (descriptor != null && DescriptorUtils.isEnumEntry(descriptor)) {
            val qualifiedParent = expression.getTopmostParentQualifiedExpressionForSelector()
            if (qualifiedParent == null || qualifiedParent.parent !is KtDoubleColonExpression) {
                trace.report(Errors.ENUM_ENTRY_AS_TYPE.on(expression))
            }
        }
    }

    fun resolveDescriptorForDoubleColonLHS(
        expression: KtExpression,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val ownerDescriptor = if (!isDebuggerContext) scope.ownerDescriptor else null

        val qualifierPartList = expression.asQualifierPartList(doubleColonLHS = true)
        if (qualifierPartList.isEmpty()) {
            return TypeQualifierResolutionResult(qualifierPartList, null)
        }

        if (qualifierPartList.size == 1) {
            val (name, simpleNameExpression) = qualifierPartList.single()
            val descriptor = scope.findClassifierAndReportDeprecationIfNeeded(
                name,
                KotlinLookupLocation(simpleNameExpression),
                simpleNameExpression,
                trace
            )
            storeResult(trace, simpleNameExpression, descriptor, ownerDescriptor, position = QualifierPosition.TYPE, isQualifier = true)
            return TypeQualifierResolutionResult(qualifierPartList, descriptor)
        }

        return resolveQualifierPartListForType(qualifierPartList, ownerDescriptor, scope, trace, isQualifier = true)
    }

    private fun KtUserType.asQualifierPartList(): Pair<List<ExpressionQualifierPart>, Boolean> {
        var hasError = false
        val result = SmartList<ExpressionQualifierPart>()
        var userType: KtUserType? = this
        while (userType != null) {
            val referenceExpression = userType.referenceExpression
            if (referenceExpression != null) {
                result.add(
                    ExpressionQualifierPart(
                        referenceExpression.getReferencedNameAsName(),
                        referenceExpression,
                        userType.typeArgumentList
                    )
                )
            } else {
                hasError = true
            }
            userType = userType.qualifier
        }
        return result.asReversed() to hasError
    }

    fun processImportReference(
        importDirective: KtImportInfo,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        excludedImportNames: Collection<FqName>,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? { // null if some error happened
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val importedReference = importDirective.importContent ?: return null
        val path = importedReference.asQualifierPartList()
        val lastPart = path.lastOrNull() ?: return null
        val packageFragmentForCheck =
            if (importDirective is KtImportDirective)
                computePackageFragmentToCheck(importDirective.containingKtFile, packageFragmentForVisibilityCheck)
            else
                null

        if (importDirective.isAllUnder) {
            val packageOrClassDescriptor = resolveToPackageOrClass(
                path, moduleDescriptor, trace, packageFragmentForCheck,
                scopeForFirstPart = null, position = QualifierPosition.IMPORT
            ).classDescriptorFromTypeAlias() ?: return null

            if (packageOrClassDescriptor is ClassDescriptor && packageOrClassDescriptor.kind.isSingleton && lastPart.expression != null) {
                trace.report(
                    Errors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON.on(
                        lastPart.expression!!,
                        packageOrClassDescriptor
                    )
                ) // todo report on star
                return null
            }

            return AllUnderImportScope.create(packageOrClassDescriptor, excludedImportNames)
        } else {
            return processSingleImport(moduleDescriptor, trace, importDirective, path, lastPart, packageFragmentForCheck)
        }
    }

    private fun DeclarationDescriptor?.classDescriptorFromTypeAlias(): DeclarationDescriptor? {
        return if (this is TypeAliasDescriptor) classDescriptor else this
    }

    private fun computePackageFragmentToCheck(
        containingFile: KtFile,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): PackageFragmentDescriptor? =
        when {
            containingFile.suppressDiagnosticsInDebugMode() -> null

            packageFragmentForVisibilityCheck is DeclarationDescriptorWithSource &&
                    packageFragmentForVisibilityCheck.source == SourceElement.NO_SOURCE -> {

                PackageFragmentWithCustomSource(
                    packageFragmentForVisibilityCheck,
                    KotlinSourceElement(containingFile)
                )
            }
            else -> packageFragmentForVisibilityCheck
        }

    private fun processSingleImport(
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        importDirective: KtImportInfo,
        path: List<QualifierPart>,
        lastPart: QualifierPart,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        val aliasName = importDirective.importedName
        if (aliasName == null) {
            // import kotlin.
            resolveToPackageOrClass(
                path,
                moduleDescriptor,
                trace,
                packageFragmentForVisibilityCheck,
                scopeForFirstPart = null,
                position = QualifierPosition.IMPORT
            )
            return null
        }

        val resolvedDescriptor = resolveToPackageOrClass(
            path.subList(0, path.size - 1), moduleDescriptor, trace,
            packageFragmentForVisibilityCheck, scopeForFirstPart = null, position = QualifierPosition.IMPORT
        ) ?: return null

        val packageOrClassDescriptor =
            (resolvedDescriptor as? TypeAliasDescriptor)?.let { it.classDescriptor ?: return null } ?: resolvedDescriptor

        return LazyExplicitImportScope(
            packageOrClassDescriptor,
            packageFragmentForVisibilityCheck,
            lastPart.name,
            aliasName,
            CallOnceFunction(Unit) { candidates ->
                if (candidates.isNotEmpty()) {
                    storeResult(
                        trace,
                        lastPart.expression,
                        candidates,
                        packageFragmentForVisibilityCheck,
                        position = QualifierPosition.IMPORT,
                        isQualifier = false
                    )
                } else {
                    tryResolveDescriptorsWhichCannotBeImported(trace, moduleDescriptor, packageOrClassDescriptor, lastPart)
                }
            }
        )
    }

    private fun tryResolveDescriptorsWhichCannotBeImported(
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor,
        packageOrClassDescriptor: DeclarationDescriptor,
        lastPart: QualifierPart
    ) {
        val lastPartExpression = lastPart.expression ?: return

        val descriptors = SmartList<DeclarationDescriptor>()
        val lastName = lastPart.name
        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageDescriptor = moduleDescriptor.getPackage(packageOrClassDescriptor.fqName.child(lastName))
                if (!packageDescriptor.isEmpty()) {
                    trace.report(Errors.PACKAGE_CANNOT_BE_IMPORTED.on(lastPartExpression))
                    descriptors.add(packageOrClassDescriptor)
                }
            }

            is ClassDescriptor -> {
                val memberScope = packageOrClassDescriptor.unsubstitutedMemberScope
                descriptors.addAll(memberScope.getContributedFunctions(lastName, lastPart.location))
                descriptors.addAll(memberScope.getContributedVariables(lastName, lastPart.location))
                if (descriptors.isNotEmpty()) {
                    trace.report(Errors.CANNOT_BE_IMPORTED.on(lastPartExpression, lastName))
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
        storeResult(
            trace,
            lastPart.expression,
            descriptors,
            shouldBeVisibleFrom = null,
            position = QualifierPosition.IMPORT,
            isQualifier = false
        )
    }

    private fun KtImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
        when (this) {
            is KtImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()
            is KtImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
        }

    private fun KtExpression.asQualifierPartList(doubleColonLHS: Boolean = false): List<ExpressionQualifierPart> {
        val result = SmartList<ExpressionQualifierPart>()

        fun addQualifierPart(expression: KtExpression?): Boolean {
            if (expression is KtSimpleNameExpression) {
                result.add(ExpressionQualifierPart(expression))
                return true
            }
            if (doubleColonLHS && expression is KtCallExpression && expression.isWithoutValueArguments) {
                val simpleName = expression.calleeExpression
                if (simpleName is KtSimpleNameExpression) {
                    result.add(ExpressionQualifierPart(simpleName.getReferencedNameAsName(), simpleName, expression.typeArgumentList))
                    return true
                }
            }
            return false
        }

        var expression: KtExpression? = this
        while (true) {
            if (addQualifierPart(expression)) break
            if (expression !is KtQualifiedExpression) break

            addQualifierPart(expression.selectorExpression)

            expression = expression.receiverExpression
        }

        return result.asReversed()
    }

    open class QualifierPart(
        val name: Name,
        val typeArguments: KtTypeArgumentList? = null,
        val location: LookupLocation = NoLookupLocation.FOR_DEFAULT_IMPORTS
    ) {
        open val expression: KtSimpleNameExpression? get() = null

        operator fun component1() = name
        open operator fun component2() = expression
        operator fun component3() = typeArguments
    }

    class ExpressionQualifierPart(
        name: Name,
        override val expression: KtSimpleNameExpression,
        typeArguments: KtTypeArgumentList? = null
    ) : QualifierPart(name, typeArguments, KotlinLookupLocation(expression)) {
        constructor(expression: KtSimpleNameExpression) : this(expression.getReferencedNameAsName(), expression)

        override fun component2() = expression
    }

    private fun resolveToPackageOrClass(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition
    ): DeclarationDescriptor? {
        val (packageOrClassDescriptor, endIndex) =
            resolveToPackageOrClassPrefix(path, moduleDescriptor, trace, shouldBeVisibleFrom, scopeForFirstPart, position)

        if (endIndex != path.size) {
            return null
        }

        return packageOrClassDescriptor
    }

    private fun resolveInIDEMode(path: List<QualifierPart>): Boolean =
        languageVersionSettings.getFlag(AnalysisFlags.ideMode) && path.size > 1 && path.first().name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

    private fun resolveToPackageOrClassPrefix(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition,
        isValue: ((KtSimpleNameExpression) -> Boolean)? = null
    ): Pair<DeclarationDescriptor?, Int> {
        if (resolveInIDEMode(path)) {
            return resolveToPackageOrClassPrefix(
                path.subList(1, path.size),
                moduleDescriptor,
                trace,
                shouldBeVisibleFrom,
                scopeForFirstPart = null,
                position = position,
                isValue = null
            ).let { it.first to it.second + 1 }
        }

        if (path.isEmpty()) {
            return Pair(moduleDescriptor.getPackage(FqName.ROOT), 0)
        }

        val firstPart = path.first()

        if (position == QualifierPosition.EXPRESSION) {
            // In expression position, value wins against classifier (and package).
            // If we see a function or variable (possibly ambiguous),
            // tell resolver we have no qualifier and let it perform the context-dependent resolution.
            if (scopeForFirstPart != null && isValue != null && firstPart.expression != null && isValue(firstPart.expression!!)) {
                return Pair(null, 0)
            }
        }

        val classifierDescriptor = scopeForFirstPart?.findClassifier(firstPart.name, firstPart.location)

        if (classifierDescriptor != null) {
            storeResult(trace, firstPart.expression, classifierDescriptor, shouldBeVisibleFrom, position)
        }

        val (prefixDescriptor, nextIndexAfterPrefix) =
            if (classifierDescriptor != null)
                Pair(classifierDescriptor, 1)
            else
                moduleDescriptor.quickResolveToPackage(path, trace, position)

        var currentDescriptor: DeclarationDescriptor? = prefixDescriptor
        for (qualifierPartIndex in nextIndexAfterPrefix until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                    is TypeAliasDescriptor -> // TODO type aliases as qualifiers? (would break some assumptions in TypeResolver)
                        null
                    is ClassDescriptor ->
                        currentDescriptor.getContributedClassifier(qualifierPart)
                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(currentDescriptor.fqName.child(qualifierPart.name))
                            } else null
                        if (packageView != null && !packageView.isEmpty()) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.getContributedClassifier(qualifierPart.name, qualifierPart.location)
                        }
                    }
                    else ->
                        null
                }

            // If we are in expression, this name can denote a value (not a package or class).
            if (!(position == QualifierPosition.EXPRESSION && nextPackageOrClassDescriptor == null)) {
                storeResult(trace, qualifierPart.expression, nextPackageOrClassDescriptor, shouldBeVisibleFrom, position)
            }

            if (nextPackageOrClassDescriptor == null) {
                return Pair(currentDescriptor, qualifierPartIndex)
            }

            currentDescriptor = nextPackageOrClassDescriptor
        }

        return Pair(currentDescriptor, path.size)
    }

    fun ClassDescriptor.getContributedClassifier(qualifierPart: QualifierPart) =
        unsubstitutedInnerClassesScope.getContributedClassifier(qualifierPart.name, qualifierPart.location)

    fun resolveNameExpressionAsQualifierForDiagnostics(
        expression: KtSimpleNameExpression,
        receiver: Receiver?,
        context: ExpressionTypingContext
    ): Qualifier? {
        val name = expression.getReferencedNameAsName()

        val location = KotlinLookupLocation(expression)
        val qualifierDescriptor = when (receiver) {
            is PackageQualifier -> {
                val childPackageFQN = receiver.descriptor.fqName.child(name)
                receiver.descriptor.module.getPackage(childPackageFQN).takeUnless { it.isEmpty() }
                    ?: receiver.descriptor.memberScope.getContributedClassifier(name, location)
            }
            is ClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)
            null -> context.scope.findClassifier(name, location)
                ?: context.scope.ownerDescriptor.module.getPackage(FqName.ROOT.child(name)).takeUnless { it.isEmpty() }
            is ReceiverValue -> receiver.type.memberScope.memberScopeAsImportingScope().findClassifier(name, location)
            else -> null
        }

        if (qualifierDescriptor != null) {
            return storeResult(context.trace, expression, qualifierDescriptor, context.scope.ownerDescriptor, QualifierPosition.EXPRESSION)
        }

        return null
    }

    data class QualifiedExpressionResolveResult(
        val classOrPackage: DeclarationDescriptor?,
        val memberName: Name?
    ) {
        companion object {
            val UNRESOLVED = QualifiedExpressionResolveResult(null, null)
        }
    }

    fun resolveClassOrPackageInQualifiedExpression(
        expression: KtQualifiedExpression,
        scope: LexicalScope,
        context: BindingContext
    ): QualifiedExpressionResolveResult {
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        val path = mapToQualifierParts(qualifiedExpressions, 0)
        val trace = DelegatingBindingTrace(context, "Temp trace for resolving qualified expression")

        val (result, index) = resolveToPackageOrClassPrefix(
            path = path,
            moduleDescriptor = scope.ownerDescriptor.module,
            trace = trace,
            shouldBeVisibleFrom = scope.ownerDescriptor,
            scopeForFirstPart = scope,
            position = QualifierPosition.EXPRESSION
        )

        if (result == null) return QualifiedExpressionResolveResult.UNRESOLVED
        return when (index) {
            path.size -> QualifiedExpressionResolveResult(result, null)
            path.size - 1 -> QualifiedExpressionResolveResult(result, path[index].name)
            else -> QualifiedExpressionResolveResult.UNRESOLVED
        }
    }

    fun resolveQualifierInExpressionAndUnroll(
        expression: KtQualifiedExpression,
        context: ExpressionTypingContext,
        isValue: (KtSimpleNameExpression) -> Boolean
    ): List<CallExpressionElement> {
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        val maxPossibleQualifierPrefix = mapToQualifierParts(qualifiedExpressions, 1)

        val nextIndexAfterPrefix = resolveToPackageOrClassPrefix(
            path = maxPossibleQualifierPrefix,
            moduleDescriptor = context.scope.ownerDescriptor.module,
            trace = context.trace,
            shouldBeVisibleFrom = context.scope.ownerDescriptor,
            scopeForFirstPart = context.scope,
            position = QualifierPosition.EXPRESSION,
            isValue = isValue
        ).second

        val nextExpressionIndexAfterQualifier =
            if (nextIndexAfterPrefix == 0) 0 else nextIndexAfterPrefix - 1

        return qualifiedExpressions
            .subList(nextExpressionIndexAfterQualifier, qualifiedExpressions.size)
            .map(::CallExpressionElement)
    }

    private fun mapToQualifierParts(
        qualifiedExpressions: List<KtQualifiedExpression>,
        skipLast: Int
    ): List<QualifierPart> {
        if (qualifiedExpressions.isEmpty()) return emptyList()

        val first = qualifiedExpressions.first()
        if (first !is KtDotQualifiedExpression) return emptyList()
        val firstReceiver = first.receiverExpression
        if (firstReceiver !is KtSimpleNameExpression) return emptyList()

        // Qualifier parts are receiver name for the leftmost expression
        //  and selector names for all but the rightmost qualified expressions
        //  (since rightmost selector should denote a value in expression position,
        //  and thus can't be a qualifier part).
        // E.g.:
        //  qualified expression 'a.b': qualifier parts == ['a']
        //  qualified expression 'a.b.c.d': qualifier parts == ['a', 'b', 'c']

        val qualifierParts = arrayListOf<QualifierPart>()
        qualifierParts.add(ExpressionQualifierPart(firstReceiver))

        for (qualifiedExpression in qualifiedExpressions.dropLast(skipLast)) {
            if (qualifiedExpression !is KtDotQualifiedExpression) break
            val selector = qualifiedExpression.selectorExpression
            if (selector !is KtSimpleNameExpression) break
            qualifierParts.add(ExpressionQualifierPart(selector))
        }

        return qualifierParts
    }

    private fun ModuleDescriptor.quickResolveToPackage(
        path: List<QualifierPart>,
        trace: BindingTrace,
        position: QualifierPosition
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize = path.indexOfFirst { it.typeArguments != null }.let { if (it == -1) path.size else it + 1 }
        var fqName = FqName.fromSegments(path.subList(0, possiblePackagePrefixSize).map { it.name.asString() })

        var prefixSize = possiblePackagePrefixSize
        while (!fqName.isRoot) {
            val packageDescriptor = getPackage(fqName)
            if (!packageDescriptor.isEmpty()) {
                recordPackageViews(path.subList(0, prefixSize), packageDescriptor, trace, position)
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
        position: QualifierPosition
    ) {
        path.foldRight(packageView) { qualifierPart, currentView ->
            storeResult(trace, qualifierPart.expression, currentView, shouldBeVisibleFrom = null, position = position)
            currentView.containingDeclaration
                ?: error(
                    "Containing Declaration must be not null for package with fqName: ${currentView.fqName}, " +
                            "path: ${path.joinToString()}, packageView fqName: ${packageView.fqName}"
                )
        }
    }

    private fun storeResult(
        trace: BindingTrace,
        referenceExpression: KtSimpleNameExpression?,
        descriptors: Collection<DeclarationDescriptor>,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        isQualifier: Boolean = true
    ) {
        referenceExpression ?: return
        if (descriptors.size > 1) {
            val visibleDescriptors = descriptors.filter { isVisible(it, shouldBeVisibleFrom, position) }
            when {
                visibleDescriptors.isEmpty() -> {
                    val descriptor = descriptors.first() as DeclarationDescriptorWithVisibility
                    trace.report(Errors.INVISIBLE_REFERENCE.on(referenceExpression, descriptor, descriptor.visibility, descriptor))
                }
                visibleDescriptors.size > 1 -> {
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, visibleDescriptors)
                }
                else -> {
                    storeResult(trace, referenceExpression, visibleDescriptors.single(), null, position, isQualifier)
                }
            }
        } else {
            storeResult(trace, referenceExpression, descriptors.singleOrNull(), shouldBeVisibleFrom, position, isQualifier)
        }
    }

    private fun storeResult(
        trace: BindingTrace,
        referenceExpression: KtSimpleNameExpression?,
        descriptor: DeclarationDescriptor?,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        isQualifier: Boolean = true
    ): Qualifier? {
        referenceExpression ?: return null
        if (descriptor == null) {
            trace.report(Errors.UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
            return null
        }

        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptor)

        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, referenceExpression, trace)

        if (descriptor is DeclarationDescriptorWithVisibility) {
            val fromToCheck =
                if (shouldBeVisibleFrom is PackageFragmentDescriptor && shouldBeVisibleFrom.source == SourceElement.NO_SOURCE && referenceExpression.containingFile !is DummyHolder) {
                    PackageFragmentWithCustomSource(shouldBeVisibleFrom, KotlinSourceElement(referenceExpression.containingKtFile))
                } else {
                    shouldBeVisibleFrom
                }
            if (!isVisible(descriptor, fromToCheck, position)) {
                trace.report(Errors.INVISIBLE_REFERENCE.on(referenceExpression, descriptor, descriptor.visibility, descriptor))
            }
        }

        return if (isQualifier) storeQualifier(trace, referenceExpression, descriptor) else null
    }

    private fun storeQualifier(
        trace: BindingTrace,
        referenceExpression: KtSimpleNameExpression,
        descriptor: DeclarationDescriptor
    ): Qualifier? {
        val qualifier =
            when (descriptor) {
                is PackageViewDescriptor -> PackageQualifier(referenceExpression, descriptor)
                is ClassDescriptor -> ClassQualifier(referenceExpression, descriptor)
                is TypeParameterDescriptor -> TypeParameterQualifier(referenceExpression, descriptor)
                is TypeAliasDescriptor -> {
                    val classDescriptor = descriptor.classDescriptor ?: return null
                    TypeAliasQualifier(referenceExpression, descriptor, classDescriptor)
                }
                else -> return null
            }

        trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)

        return qualifier
    }

    companion object {
        /**
         *  Shouldn't be visible to users.
         *  Used as prefix for [FqName] from non-root to avoid conflicts when resolving in IDE.
         *  E.g.:
         *  ---------
         *  package a
         *
         *  class A
         *
         *  fun test(a: Any) {
         *      a.A() // invalid code -> incorrect import/completion/etc.
         *      _root_ide_package_.a.A() // OK
         *  }
         *  ---------
         */
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE = "_root_ide_package_"
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT = "$ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE."
    }
}

internal fun isVisible(
    descriptor: DeclarationDescriptor,
    shouldBeVisibleFrom: DeclarationDescriptor?,
    position: QualifierPosition
): Boolean {
    if (descriptor !is DeclarationDescriptorWithVisibility || shouldBeVisibleFrom == null) return true

    val visibility = descriptor.visibility
    if (position == QualifierPosition.IMPORT) {
        if (Visibilities.isPrivate(visibility)) return Visibilities.inSameFile(descriptor, shouldBeVisibleFrom)
        if (!visibility.mustCheckInImports()) return true
    }
    return Visibilities.isVisibleIgnoringReceiver(descriptor, shouldBeVisibleFrom)
}

internal enum class QualifierPosition {
    PACKAGE_HEADER, IMPORT, TYPE, EXPRESSION
}

/*
    This purpose of this class is to pass information about source file for current package fragment in order for check visibilities between modules
    (see ModuleVisibilityHelperImpl.isInFriendModule).
 */
private class PackageFragmentWithCustomSource(private val original: PackageFragmentDescriptor, private val source: SourceElement) :
    PackageFragmentDescriptor by original {
    override fun getSource(): SourceElement = source
}
