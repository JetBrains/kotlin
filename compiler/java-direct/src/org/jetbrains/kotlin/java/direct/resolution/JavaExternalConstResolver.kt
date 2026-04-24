/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves cross-language `const val` references appearing in Java field initializers and
 * annotation arguments.
 */

/**
 * Resolves an external field reference (e.g. a Kotlin `const val`) referenced from a Java field
 * initializer to its compile-time constant value. Tries, in order: top-level property exposed via
 * a JVM facade class (`MainKt.FOO`), class member property, companion-object property. Returns
 * `null` if [classQualifier] is `null` (unqualified — not supported across languages) or if none
 * of the cases resolves to a const value.
 */
internal fun FirSession.resolveExternalFieldValue(
    classQualifier: String?,
    fieldName: String,
    currentPackage: FqName,
): Any? {
    if (classQualifier == null) return null
    val propertyName = Name.identifier(fieldName)
    val lastDotIndex = classQualifier.lastIndexOf('.')
    val qualifierPackage = if (lastDotIndex == -1) currentPackage else FqName(classQualifier.substring(0, lastDotIndex))

    tryResolveAsTopLevel(qualifierPackage, propertyName)?.let { return it }

    val classIds = if (lastDotIndex == -1) {
        listOf(ClassId(currentPackage, Name.identifier(classQualifier)), ClassId.topLevel(FqName(classQualifier)))
    } else {
        listOf(ClassId.topLevel(FqName(classQualifier)))
    }

    return tryResolveAsClassMember(classIds, propertyName)
        ?: tryResolveAsCompanionMember(classIds, propertyName)
}

/** Top-level Kotlin property exposed via a JVM facade class (e.g. `MainKt.FOO`). */
private fun FirSession.tryResolveAsTopLevel(qualifierPackage: FqName, propertyName: Name): Any? {
    val provider = nullableSymbolProvider ?: return null
    for (symbol in provider.getTopLevelPropertySymbols(qualifierPackage, propertyName)) {
        symbol.tryExtractConstantValue(this)?.let { return it }
    }
    return null
}

/** Direct class member property (e.g. `object Foo { const val BAR = 1 }`, or a Java static field on a Kotlin class). */
private fun FirSession.tryResolveAsClassMember(classIds: List<ClassId>, propertyName: Name): Any? {
    if (nullableSymbolProvider == null) return null
    for (classId in classIds) {
        for (symbol in getClassDeclaredPropertySymbols(classId, propertyName)) {
            symbol.tryExtractConstantValue(this)?.let { return it }
        }
    }
    return null
}

/** Companion-object property (e.g. `Foo.BAR` where `BAR` lives in `Foo.Companion`). */
@OptIn(SymbolInternals::class)
private fun FirSession.tryResolveAsCompanionMember(classIds: List<ClassId>, propertyName: Name): Any? {
    val provider = nullableSymbolProvider ?: return null
    for (classId in classIds) {
        val classSymbol = provider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
        val companionClassId = classSymbol?.companionObjectSymbol?.classId ?: continue
        for (symbol in getClassDeclaredPropertySymbols(companionClassId, propertyName)) {
            symbol.tryExtractConstantValue(this)?.let { return it }
        }
    }
    return null
}

/**
 * Tries to resolve a reference inside an annotation argument as a const field value. Checks, in
 * order: enum-class companion only, class member, class companion member, top-level facade.
 * Returns `null` when [classId] is unknown to the session or no lookup produces a `const val`
 * initializer.
 */
@OptIn(SymbolInternals::class)
internal fun FirSession.resolveConstFieldValue(classId: ClassId, fieldName: Name): Any? {
    val classSymbol = cycleSafeClassLikeSymbol(classId) as? FirRegularClassSymbol

    if (classSymbol != null) {
        // Enum classes only expose const properties through their companion (entries are
        // FirEnumEntry, not FirProperty). The top-level/facade fallback also doesn't apply to
        // an `<EnumClass>.X` shape — that always denotes an enum entry of `<EnumClass>`.
        if (classSymbol.classKind == ClassKind.ENUM_CLASS) {
            val companionClassId = classSymbol.companionObjectSymbol?.classId ?: return null
            return resolveConstPropertyValueInClass(companionClassId, fieldName)
        }

        // Class member first, companion second: a reference like `MainKt.FOO` resolves `MainKt`
        // as a real class before the Kotlin facade fallback, so a genuine class/companion
        // member of the same name must win over the top-level property.
        resolveConstPropertyValueInClass(classId, fieldName)?.let { return it }
        val companionClassId = classSymbol.companionObjectSymbol?.classId
        if (companionClassId != null) {
            resolveConstPropertyValueInClass(companionClassId, fieldName)?.let { return it }
        }
    }

    // Fallback: top-level Kotlin property exposed via the facade class (e.g., MainKt.FOO →
    // top-level const val FOO).
    return tryResolveAsTopLevel(classId.packageFqName, fieldName)
}

/**
 * Reads a `const val` property declared on [classId] (by simple name [propertyName]) through
 * [getClassDeclaredPropertySymbols] and returns its evaluated value, or `null` when no const
 * property of that name exists.
 */
@OptIn(SymbolInternals::class)
private fun FirSession.resolveConstPropertyValueInClass(classId: ClassId, propertyName: Name): Any? {
    if (nullableSymbolProvider == null) return null
    for (symbol in getClassDeclaredPropertySymbols(classId, propertyName)) {
        if (symbol is FirPropertySymbol && symbol.isConst) {
            extractEvaluatedConstValue(symbol.fir, this)?.let { return it }
        }
    }
    return null
}

/**
 * Extracts a constant value from a property or Java field symbol.
 *
 * `resolvedInitializer` is read first to cover Java-field symbols whose initializer was already
 * evaluated by the Java loader; the `FirPropertySymbol && isConst` branch covers Kotlin
 * `const val`s via [FirExpressionEvaluator].
 */
@OptIn(SymbolInternals::class)
private fun FirVariableSymbol<*>.tryExtractConstantValue(session: FirSession): Any? {
    (resolvedInitializer as? FirLiteralExpression)?.let { return it.value }
    if (this is FirPropertySymbol && isConst) {
        return extractEvaluatedConstValue(fir, session)
    }
    return null
}

/** Extracts the evaluated constant value from a `const` [FirProperty] using FIR's const evaluator. */
private fun extractEvaluatedConstValue(property: FirProperty, session: FirSession): Any? {
    (property.initializer as? FirLiteralExpression)?.let { return it.value }

    val evaluated = property.evaluatedInitializer
        ?: FirExpressionEvaluator.evaluatePropertyInitializer(property, session)

    return ((evaluated as? FirEvaluatorResult.Evaluated)?.result as? FirLiteralExpression)?.value
}
