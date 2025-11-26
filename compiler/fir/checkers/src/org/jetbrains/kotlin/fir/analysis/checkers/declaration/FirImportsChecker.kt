/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirDeprecationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.toInvisibleReferenceDiagnostic
import org.jetbrains.kotlin.fir.analysis.getLastImportedFqNameSegmentSource
import org.jetbrains.kotlin.fir.analysis.getSourceForImportSegment
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker

object FirImportsChecker : FirFileChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        declaration.imports.forEach { import ->
            if (import.source?.kind?.shouldSkipErrorTypeReporting == true) return@forEach
            when {
                import.isAllUnder && import is FirResolvedImport -> checkAllUnderFromObject(import)
                import.isAllUnder -> checkAllUnderFromEnumEntry(import)
                import.isPackage -> checkIsPackage(import)
                else -> {
                    checkCanBeImported(import)
                    if (import is FirResolvedImport) {
                        checkOperatorRename(import)
                    }
                }
            }
            checkImportApiStatus(import)
        }
        checkConflictingImports(declaration.imports)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAllUnderFromEnumEntry(import: FirImport) {
        val fqName = import.importedFqName ?: return
        if (fqName.isRoot || fqName.parent().isRoot) return
        val classId = ClassId.topLevel(fqName.parent())
        val classSymbol = classId.resolveToClass() ?: return
        if (classSymbol.isEnumClass && classSymbol.collectEnumEntries(context.session).any {
                it.callableId.callableName == fqName.shortName()
            }
        ) {
            reporter.reportOn(import.source, FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, classSymbol.classId.shortClassName)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAllUnderFromObject(import: FirImport) {
        val fqName = import.importedFqName ?: return
        if (fqName.isRoot) return
        val classLike = when (val resolutionResult = resolveToPackageOrClass(context.session.symbolProvider, fqName)) {
            is PackageResolutionResult.PackageOrClass -> resolutionResult.classSymbol ?: return
            // Already an error import, already reported
            is PackageResolutionResult.Error -> return
        }
        val classSymbol = classLike.fullyExpandedClass()
        if (classSymbol != null && classSymbol.classKind.isObject) {
            reporter.reportOn(import.source, FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, classSymbol.classId.shortClassName)
        }
        if (!classLike.isVisible()) {
            val source = import.getLastImportedFqNameSegmentSource() ?: error("`${import.source}` does not contain `$fqName`")
            reporter.report(classLike.toInvisibleReferenceDiagnostic(source, context.session), context)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkIsPackage(import: FirImport) {
        val fqName = import.importedFqName ?: return
        if (context.session.symbolProvider.hasPackage(fqName)) return
        when (val resolutionResult = resolveToPackageOrClass(context.session.symbolProvider, fqName)) {
            is PackageResolutionResult.PackageOrClass if resolutionResult.classSymbol != null ->
                reporter.reportOn(import.source, FirErrors.CANNOT_IMPORT_AS_PACKAGE)
            else ->
                reporter.reportOn(import.source, FirErrors.UNRESOLVED_IMPORT, fqName.asString())
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCanBeImported(import: FirImport) {
        val importedFqName = import.importedFqName ?: return
        val importedName = importedFqName.shortName()
        //empty name come from LT in some erroneous cases
        if (importedName.isSpecial || importedName.identifier.isEmpty()) return

        val symbolProvider = context.session.symbolProvider
        val parentClassId = (import as? FirResolvedImport)?.resolvedParentClassId
        if (parentClassId != null) {
            val parentClassLikeSymbol = parentClassId.resolveToClassLike() ?: return
            val parentClassSymbol = parentClassLikeSymbol.fullyExpandedClass() ?: return

            fun reportInvisibleParentClasses(classSymbol: FirClassLikeSymbol<*>, depth: Int) {
                if (!classSymbol.isVisible()) {
                    val source = import.getSourceForImportSegment(indexFromLast = depth)
                    reporter.report(classSymbol.toInvisibleReferenceDiagnostic(source, context.session), context)
                }
            }

            fun reportInvisibleParentClassesRecursively(classSymbol: FirRegularClassSymbol, depth: Int) {
                reportInvisibleParentClasses(classSymbol, depth)
                classSymbol.classId.outerClassId?.resolveToClass()?.let {
                    reportInvisibleParentClassesRecursively(it, depth + 1)
                }
            }

            if (parentClassLikeSymbol is FirTypeAliasSymbol) {
                // Checking one outer typealias only is enough, because we don't support nested typealiases.
                reportInvisibleParentClasses(parentClassLikeSymbol, depth = 1)
            }
            reportInvisibleParentClassesRecursively(parentClassSymbol, 1)

            when (val status = parentClassSymbol.getImportStatusOfCallableMembers(importedName)) {
                ImportStatus.OK -> {
                    if (parentClassLikeSymbol is FirTypeAliasSymbol) {
                        reporter.reportOn(
                            import.source,
                            FirErrors.TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT,
                            parentClassLikeSymbol.name,
                            parentClassSymbol.name
                        )
                    }
                }
                is ImportStatus.Invisible -> {
                    val source = import.getSourceForImportSegment(0)
                    reporter.report(status.symbol.toInvisibleReferenceDiagnostic(source, context.session), context)
                }
                else -> {
                    val classId = parentClassSymbol.classId.createNestedClassId(importedName)
                    if (symbolProvider.getClassLikeSymbolByClassId(classId) != null) return
                    if (status == ImportStatus.Unresolved) {
                        reporter.reportOn(import.source, FirErrors.UNRESOLVED_IMPORT, importedName.asString())
                    } else {
                        reporter.reportOn(import.source, FirErrors.CANNOT_BE_IMPORTED, importedName)
                    }
                }
            }
            return
        }

        var resolvedDeclaration: FirBasedSymbol<*>? = null

        ClassId.topLevel(importedFqName).resolveToClass()?.let {
            resolvedDeclaration = it

            if (it.isVisible()) {
                return
            }
        }

        // Note: two checks below are both heavyweight, so we should do them lazily!

        val topLevelCallableSymbol = symbolProvider.getTopLevelCallableSymbols(importedFqName.parent(), importedName)

        for (it in topLevelCallableSymbol) {
            if (it.isVisible()) {
                return
            }

            if (resolvedDeclaration == null) {
                resolvedDeclaration = it
            }
        }

        resolvedDeclaration?.let {
            val source = import.getSourceForImportSegment(0) ?: import.source
            reporter.report(it.toInvisibleReferenceDiagnostic(source, context.session), context)
            return
        }

        if (symbolProvider.hasPackage(importedFqName)) {
            reporter.reportOn(import.source, FirErrors.PACKAGE_CANNOT_BE_IMPORTED)
        } else {
            reporter.reportOn(import.source, FirErrors.UNRESOLVED_IMPORT, importedName.asString())
        }
    }

    context(context: CheckerContext)
    private fun FirBasedSymbol<*>.isVisible(): Boolean {
        val useSiteFileSymbol = context.containingFileSymbol ?: return false
        val visibility = when (this) {
            is FirCallableSymbol<*> -> this.visibility
            is FirClassLikeSymbol<*> -> this.visibility
            else -> return false
        }

        if (visibility != Visibilities.Unknown && !visibility.mustCheckInImports()) return true
        if (visibility == Visibilities.Private || visibility == Visibilities.PrivateToThis) {
            return useSiteFileSymbol == context.session.firProvider.getContainingFile(this)?.symbol
        }

        return context.session.visibilityChecker.isVisible(
            this,
            context.session,
            useSiteFileSymbol,
            emptyList(),
            null,
            skipCheckForContainingClassVisibility = true,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConflictingImports(imports: List<FirImport>) {
        val interestingImports = imports
            .filterIsInstanceWithChecker<FirResolvedImport> { import ->
                !import.isAllUnder &&
                        import.source?.kind?.shouldSkipErrorTypeReporting != true &&
                        import.importedName?.identifierOrNullIfSpecial?.isNotEmpty() == true &&
                        import.resolvesToClass()
            }.filterNot { it.source?.kind == KtFakeSourceElementKind.ImplicitImport }
        interestingImports
            .groupBy { it.aliasName ?: it.importedName!! }
            .values
            .filter { it.size > 1 }
            .forEach { conflicts ->
                conflicts.forEach {
                    reporter.reportOn(it.source, FirErrors.CONFLICTING_IMPORT, it.importedName!!)
                }
            }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOperatorRename(import: FirResolvedImport) {
        val alias = import.aliasName ?: return
        val importedName = import.importedName ?: return
        if (!OperatorConventions.isConventionName(alias)) return
        val classId = import.resolvedParentClassId
        val illegalRename = if (classId != null) {
            val classFir = classId.resolveToClass() ?: return
            classFir.classKind.isSingleton && classFir.hasFunction(importedName) { it.isOperator }
        } else {
            context.session.symbolProvider.getTopLevelFunctionSymbols(import.packageFqName, importedName).any {
                it.isOperator
            }
        }
        if (illegalRename) {
            reporter.reportOn(import.source, FirErrors.OPERATOR_RENAMED_ON_IMPORT)
        }
    }

    context(context: CheckerContext)
    private fun FirResolvedImport.resolvesToClass(): Boolean {
        if (resolvedParentClassId != null) {
            if (isAllUnder) return true
            val parentClass = resolvedParentClassId!!
            val relativeClassName = this.relativeParentClassName ?: return false
            val importedName = this.importedName ?: return false
            val innerClassId = ClassId(parentClass.packageFqName, relativeClassName.child(importedName), isLocal = false)
            return innerClassId.resolveToClass() != null
        } else {
            val importedFqName = importedFqName ?: return false
            if (importedFqName.isRoot) return false
            val importedClassId = ClassId.topLevel(importedFqName)
            return importedClassId.resolveToClass() != null
        }
    }

    context(context: CheckerContext)
    private fun ClassId.resolveToClassLike(): FirClassLikeSymbol<*>? {
        return context.session.symbolProvider.getClassLikeSymbolByClassId(this)
    }

    context(context: CheckerContext)
    private fun ClassId.resolveToClass(): FirRegularClassSymbol? {
        val classSymbol = resolveToClassLike() ?: return null
        return when (classSymbol) {
            is FirRegularClassSymbol -> classSymbol
            is FirTypeAliasSymbol -> classSymbol.fullyExpandedClass()
            is FirAnonymousObjectSymbol -> null
        }
    }

    context(context: CheckerContext)
    private fun FirRegularClassSymbol.hasFunction(
        name: Name,
        predicate: (FirNamedFunctionSymbol) -> Boolean
    ): Boolean {
        var result = false
        context.session.declaredMemberScope(this, memberRequiredPhase = null).processFunctionsByName(name) { sym ->
            if (!result) {
                result = predicate(sym)
            }
        }
        return result
    }

    private sealed class ImportStatus {
        data object OK : ImportStatus()
        data class Invisible(val symbol: FirCallableSymbol<*>) : ImportStatus()
        data object CannotBeImported : ImportStatus()
        data object Unresolved : ImportStatus()
    }

    context(context: CheckerContext)
    private fun FirRegularClassSymbol.getImportStatusOfCallableMembers(name: Name): ImportStatus {
        return if (classKind.isSingleton) {
            // Use declaredMemberScope first because it's faster, and it's relatively rare to import members declared from super types.
            val scopes = listOf(context.session.declaredMemberScope(this, memberRequiredPhase = null), unsubstitutedScope())
            getImportStatus(scopes, name) { true }
        } else {
            val scopes = listOfNotNull(
                // We first try resolution with declaredMemberScope because it's faster and typically imported members are not from
                // super types.
                context.session.declaredMemberScope(this, memberRequiredPhase = null),

                // Next, we try static scope, which can provide static (Java) members from super classes. Note that it's not available
                // for pure Kotlin classes.
                staticScope(context.sessionHolder),

                // Finally, we fall back to unsubstitutedScope to catch all
                unsubstitutedScope()
            )
            getImportStatus(scopes, name) { it.isStatic }
        }
    }

    context(context: CheckerContext)
    private inline fun getImportStatus(
        scopes: List<FirContainingNamesAwareScope>,
        name: Name,
        crossinline isApplicable: (FirCallableSymbol<*>) -> Boolean
    ): ImportStatus {
        var found = false
        var symbol: FirCallableSymbol<*>? = null

        for (scope in scopes) {
            scope.processFunctionsByName(name) { sym ->
                if (sym.isVisible() && isApplicable(sym)) found = true
                symbol = sym
            }
            if (found) return ImportStatus.OK

            scope.processPropertiesByName(name) { sym ->
                if (sym.isVisible() && isApplicable(sym)) found = true
                symbol = sym
            }
            if (found) return ImportStatus.OK
        }

        return when {
            symbol?.let(isApplicable) == true -> ImportStatus.Invisible(symbol)
            symbol != null -> ImportStatus.CannotBeImported
            else -> ImportStatus.Unresolved
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkImportApiStatus(import: FirImport) {
        val importedFqName = import.importedFqName ?: return
        if (importedFqName.isRoot || importedFqName.shortName().asString().isEmpty()) return
        val classId = (import as? FirResolvedImport)?.resolvedParentClassId ?: ClassId.topLevel(importedFqName)
        val symbol = classId.toSymbol() ?: return
        FirDeprecationChecker.reportApiStatusIfNeeded(import.source, symbol)
    }
}
