/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.collectEnumEntries
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirDeprecationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirErrorImport
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isStatic
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.expressions.OperatorConventions

object FirImportsChecker : FirFileChecker() {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.imports.forEach { import ->
            if (import is FirErrorImport) return@forEach
            if (import.isAllUnder) {
                if (import !is FirResolvedImport) {
                    checkAllUnderFromEnumEntry(import, context, reporter)
                }
            } else {
                checkCanBeImported(import, context, reporter)
                if (import is FirResolvedImport) {
                    checkOperatorRename(import, context, reporter)
                }
            }
            checkDeprecatedImport(import, context, reporter)
        }
        checkConflictingImports(declaration.imports, context, reporter)
    }

    private fun checkAllUnderFromEnumEntry(import: FirImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val fqName = import.importedFqName ?: return
        if (fqName.isRoot || fqName.parent().isRoot) return
        val classId = ClassId.topLevel(fqName.parent())
        val classSymbol = classId.resolveToClass(context) ?: return
        if (classSymbol.isEnumClass && classSymbol.collectEnumEntries().any { it.callableId.callableName == fqName.shortName() }) {
            reporter.reportOn(import.source, FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, classSymbol.classId.shortClassName, context)
        }
    }

    private fun checkCanBeImported(import: FirImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val importedFqName = import.importedFqName ?: return
        val importedName = importedFqName.shortName()
        //empty name come from LT in some erroneous cases
        if (importedName.isSpecial || importedName.identifier.isEmpty()) return

        val classId = (import as? FirResolvedImport)?.resolvedParentClassId
        if (classId != null) {
            val classSymbol = classId.resolveToClass(context) ?: return
            if (classSymbol.classKind.isSingleton) return

            if (!classSymbol.canBeImported(context, importedName)) {
                reporter.reportOn(import.source, FirErrors.CANNOT_BE_IMPORTED, importedName, context)
            }
        } else {
            val importedClassId = ClassId.topLevel(importedFqName)
            if (importedClassId.resolveToClass(context) != null
                || context.session.symbolProvider.getTopLevelCallableSymbols(importedFqName.parent(), importedName).isNotEmpty()
            ) {
                return
            }
            context.session.symbolProvider.getPackage(importedFqName)?.let {
                reporter.reportOn(import.source, FirErrors.PACKAGE_CANNOT_BE_IMPORTED, context)
            }
        }
    }

    private fun checkConflictingImports(imports: List<FirImport>, context: CheckerContext, reporter: DiagnosticReporter) {
        val interestingImports = imports
            .filterIsInstance<FirResolvedImport>()
            .filter { import ->
                !import.isAllUnder
                        && import.importedName?.identifierOrNullIfSpecial?.isNotEmpty() == true
                        && import.resolvesToClass(context)
            }
        interestingImports
            .groupBy { it.aliasName ?: it.importedName!! }
            .values
            .filter { it.size > 1 }
            .forEach { conflicts ->
                conflicts.forEach {
                    reporter.reportOn(it.source, FirErrors.CONFLICTING_IMPORT, it.importedName!!, context)
                }
            }
    }

    private fun checkOperatorRename(import: FirResolvedImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val alias = import.aliasName ?: return
        val importedName = import.importedName ?: return
        if (!OperatorConventions.isConventionName(alias)) return
        val classId = import.resolvedParentClassId
        val illegalRename = if (classId != null) {
            val classFir = classId.resolveToClass(context) ?: return
            classFir.classKind.isSingleton && classFir.hasFunction(context, importedName) { it.isOperator }
        } else {
            context.session.symbolProvider.getTopLevelFunctionSymbols(import.packageFqName, importedName).any {
                it.isOperator
            }
        }
        if (illegalRename) {
            reporter.reportOn(import.source, FirErrors.OPERATOR_RENAMED_ON_IMPORT, context)
        }
    }

    private fun FirResolvedImport.resolvesToClass(context: CheckerContext): Boolean {
        if (resolvedParentClassId != null) {
            if (isAllUnder) return true
            val parentClass = resolvedParentClassId!!
            val relativeClassName = this.relativeParentClassName ?: return false
            val importedName = this.importedName ?: return false
            val innerClassId = ClassId(parentClass.packageFqName, relativeClassName.child(importedName), false)
            return innerClassId.resolveToClass(context) != null
        } else {
            val importedFqName = importedFqName ?: return false
            if (importedFqName.isRoot) return false
            val importedClassId = ClassId.topLevel(importedFqName)
            return importedClassId.resolveToClass(context) != null
        }
    }

    private fun ClassId.resolveToClass(context: CheckerContext): FirRegularClassSymbol? {
        val classSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(this) ?: return null
        return when (classSymbol) {
            is FirRegularClassSymbol -> classSymbol
            is FirTypeAliasSymbol -> classSymbol.fullyExpandedClass(context.session)
            else -> null
        }
    }

    private fun FirRegularClassSymbol.hasFunction(
        context: CheckerContext,
        name: Name,
        predicate: (FirNamedFunctionSymbol) -> Boolean
    ): Boolean {
        var result = false
        context.session.declaredMemberScope(this).processFunctionsByName(name) { sym ->
            if (!result) {
                result = predicate(sym)
            }
        }
        return result
    }

    private fun FirRegularClassSymbol.canBeImported(
        context: CheckerContext,
        name: Name
    ): Boolean {
        var hasStatic = false
        var hasIllegal = false
        val scope = context.session.declaredMemberScope(this)
        scope.processFunctionsByName(name) { sym ->
            if (sym.isStatic) hasStatic = true
            else hasIllegal = true
        }
        if (hasStatic) return true
        if (hasIllegal) return false

        scope.processPropertiesByName(name) { sym ->
            if (sym.isStatic) hasStatic = true
            else hasIllegal = true
        }

        return hasStatic || !hasIllegal
    }

    private fun checkDeprecatedImport(import: FirImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val importedFqName = import.importedFqName ?: return
        if (importedFqName.isRoot || importedFqName.shortName().asString().isEmpty()) return
        val classId = (import as? FirResolvedImport)?.resolvedParentClassId ?: ClassId.topLevel(importedFqName)
        val classLike: FirRegularClassSymbol = classId.resolveToClass(context) ?: return
        FirDeprecationChecker.reportDeprecationIfNeeded(import.source, classLike, null, context, reporter)
    }
}
