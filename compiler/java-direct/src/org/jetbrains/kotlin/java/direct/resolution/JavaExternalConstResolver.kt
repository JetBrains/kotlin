/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

/**
 * Resolves cross-language `const val` references appearing in Java field initializers and
 * annotation arguments.
 */

/**
 * Resolves an external field reference (e.g. a Kotlin `const val`) referenced from a Java field
 * initializer to its compile-time constant value. When [qualifierClassId] is null, it looks for a
 * top-level property exposed through a JVM facade class (`MainKt.FOO`); otherwise it looks for a
 * class member property and then a companion-object property. Returns `null` if no case resolves
 * to a const value.
 *
 * [qualifierClassId] is the class [classQualifier] resolves to, if any. A qualified name like
 * `p.X.Y` is classified left to right (JLS 6.5.4.2), so it may denote the nested class `p.X.Y`,
 * and it never falls back to a class `Y` of a package `p.X` once `p.X` is a class. Without a
 * class, [classQualifier] can still name a JVM facade class, which is split at its last dot.
 */
internal fun FirSession.resolveExternalFieldValue(
    classQualifier: String,
    qualifierClassId: ClassId?,
    fieldName: String,
    currentPackage: FqName,
): Any? {
    if (nullableSymbolProvider == null) return null
    val propertyName = Name.identifier(fieldName)
    if (qualifierClassId == null) {
        val lastDotIndex = classQualifier.lastIndexOf('.')
        val qualifierPackage = if (lastDotIndex == -1) currentPackage else FqName(classQualifier.substring(0, lastDotIndex))
        val qualifierClassName = classQualifier.substring(lastDotIndex + 1)
        return tryResolveAsTopLevelPropertyValue(qualifierPackage, qualifierClassName, propertyName)
    }

    return tryResolveAsClassMember(qualifierClassId, propertyName)
        ?: tryResolveAsCompanionMember(qualifierClassId, propertyName)
}

/**
 * Top-level Kotlin property compiled into the JVM facade class [qualifierClassName] (e.g. `MainKt.FOO`).
 * The facade check guards `codegen/box/javaDirect/javaConstantQualifiedByJavaClassVsKotlinTopLevelConst.kt`.
 */
private fun FirSession.tryResolveAsTopLevelPropertyValue(qualifierPackage: FqName, qualifierClassName: String, propertyName: Name): Any? {
    val provider = nullableSymbolProvider ?: return null
    for (symbol in provider.getTopLevelPropertySymbols(qualifierPackage, propertyName)) {
        if (symbol.jvmFacadeClassName() != qualifierClassName) continue
        symbol.tryExtractConstantValue(this)?.let { return it }
    }
    return null
}

/**
 * Name of the JVM facade class the property is compiled into: the declaring file's `@JvmName`
 * argument, otherwise its capitalized file name plus `Kt`. `null` if the declaring file is not reachable.
 */
@OptIn(SymbolInternals::class)
private fun FirVariableSymbol<*>.jvmFacadeClassName(): String? {
    (containerSource as? JvmPackagePartSource)?.let { source ->
        return (source.facadeClassName ?: source.className).internalName.substringAfterLast('/')
    }
    val file = fir.moduleData.session.nullableFirProvider?.getFirCallableContainerFile(this) ?: return null
    return file.findJvmNameValue() ?: (file.name.removeSuffix(".kt").capitalizeAsciiOnly() + "Kt")
}

/** Direct class member property (e.g. `object Foo { const val BAR = 1 }`, or a Java static field on a Kotlin class). */
private fun FirSession.tryResolveAsClassMember(classId: ClassId, propertyName: Name): Any? {
    if (nullableSymbolProvider == null) return null
    for (symbol in getClassDeclaredPropertySymbols(classId, propertyName)) {
        symbol.tryExtractConstantValue(this)?.let { return it }
    }
    return null
}

/** Companion-object property (e.g. `Foo.BAR` where `BAR` lives in `Foo.Companion`). */
@OptIn(SymbolInternals::class)
private fun FirSession.tryResolveAsCompanionMember(classId: ClassId, propertyName: Name): Any? {
    val provider = nullableSymbolProvider ?: return null
    val classSymbol = provider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
    val companionClassId = classSymbol?.companionObjectSymbol?.classId ?: return null
    for (symbol in getClassDeclaredPropertySymbols(companionClassId, propertyName)) {
        symbol.tryExtractConstantValue(this)?.let { return it }
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

/**
 * Whether the class [classId], known to the session as a FIR class (not a Java source class of the
 * current module), is an enum declaring the entry [entryName].
 */
internal fun FirSession.hasEnumEntry(classId: ClassId, entryName: String): Boolean {
    val classSymbol = cycleSafeClassLikeSymbol(classId) as? FirRegularClassSymbol ?: return false
    if (classSymbol.classKind != ClassKind.ENUM_CLASS) return false
    return classSymbol.collectEnumEntries(this).any { it.name.asString() == entryName }
}
