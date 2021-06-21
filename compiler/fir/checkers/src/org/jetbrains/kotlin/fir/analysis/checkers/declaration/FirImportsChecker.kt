/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.followAllAlias
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
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
            if (import.isAllUnder && import !is FirResolvedImport) {
                checkAllUnderFromEnumEntry(import, context, reporter)
            }
            if (!import.isAllUnder) {
                checkCanBeImported(import, context, reporter)
                if (import is FirResolvedImport) {
                    checkOperatorRename(import, context, reporter)
                }
            }
        }
        checkConflictingImports(declaration.imports, context, reporter)
    }

    private fun checkAllUnderFromEnumEntry(import: FirImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val fqName = import.importedFqName ?: return
        if (fqName.isRoot || fqName.parent().isRoot) return
        val classId = ClassId.topLevel(fqName.parent())
        val classFir = classId.resolveToClass(context) ?: return
        if (classFir.isEnumClass && classFir.collectEnumEntries().any { it.name == fqName.shortName() }) {
            reporter.reportOn(import.source, FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, classFir.name, context)
        }
    }

    private fun checkCanBeImported(import: FirImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val importedFqName = import.importedFqName ?: return
        val importedName = importedFqName.shortName()
        //empty name come from LT in some erroneous cases
        if (importedName.isSpecial || importedName.identifier.isEmpty()) return

        val classId = (import as? FirResolvedImport)?.resolvedClassId
        if (classId != null) {
            val classFir = classId.resolveToClass(context) ?: return
            if (classFir.classKind.isSingleton) return

            if (!classFir.canBeImported(context, importedName)) {
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
        val classId = import.resolvedClassId
        val illegalRename = if (classId != null) {
            val classFir = classId.resolveToClass(context) ?: return
            classFir.classKind.isSingleton && classFir.hasFunction(context, importedName) { it.isOperator }
        } else {
            context.session.symbolProvider.getTopLevelFunctionSymbols(import.packageFqName, importedName).any {
                it.fir.isOperator
            }
        }
        if (illegalRename) {
            reporter.reportOn(import.source, FirErrors.OPERATOR_RENAMED_ON_IMPORT, context)
        }
    }

    private fun FirResolvedImport.resolvesToClass(context: CheckerContext): Boolean {
        if (resolvedClassId != null) {
            if (isAllUnder) return true
            val parentClass = resolvedClassId!!
            val relativeClassName = this.relativeClassName ?: return false
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

    private fun ClassId.resolveToClass(context: CheckerContext): FirRegularClass? {
        val classSymbol = context.session.symbolProvider.getClassLikeSymbolByFqName(this) ?: return null
        return when (classSymbol) {
            is FirRegularClassSymbol -> classSymbol.fir
            is FirTypeAliasSymbol -> classSymbol.fir.followAllAlias(context.session) as? FirRegularClass
            else -> null
        }
    }

    private fun FirRegularClass.hasFunction(context: CheckerContext, name: Name, predicate: (FirSimpleFunction) -> Boolean): Boolean {
        var result = false
        context.session.declaredMemberScope(this).processFunctionsByName(name) { sym ->
            if (!result) {
                result = predicate(sym.fir)
            }
        }
        return result
    }

    private fun FirRegularClass.canBeImported(
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
}
