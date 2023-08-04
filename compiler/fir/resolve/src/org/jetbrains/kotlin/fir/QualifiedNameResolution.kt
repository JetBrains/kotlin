/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.getDeprecationForCallSite
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.getSingleVisibleClassifier
import org.jetbrains.kotlin.fir.resolve.createCurrentScopeList
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDeprecated
import org.jetbrains.kotlin.fir.resolve.setTypeOfQualifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE = "_root_ide_package_"

fun BodyResolveComponents.resolveRootPartOfQualifier(
    namedReference: FirSimpleNamedReference,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
): FirResolvedQualifier? {
    val name = namedReference.name
    if (name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) {
        return buildResolvedQualifier {
            this.source = qualifiedAccess.source
            packageFqName = FqName.ROOT
            this.nonFatalDiagnostics.addAll(nonFatalDiagnosticsFromExpression.orEmpty())
            annotations += qualifiedAccess.annotations
        }.apply {
            setTypeOfQualifier(session)
        }
    }

    for (scope in createCurrentScopeList()) {
        scope.getSingleVisibleClassifier(session, this, name)?.let {
            val klass = (it as? FirClassLikeSymbol<*>)?.fullyExpandedClass(session)
                ?: return@let

            val isVisible = session.visibilityChecker.isClassLikeVisible(
                klass.fir,
                session,
                file,
                containingDeclarations,
            )
            if (!isVisible) {
                return@let
            }
            val classId = it.classId
            return buildResolvedQualifier {
                this.source = qualifiedAccess.source
                packageFqName = classId.packageFqName
                relativeClassFqName = classId.relativeClassName
                symbol = it
                this.typeArguments.addAll(qualifiedAccess.typeArguments)
                this.nonFatalDiagnostics.addAll(
                    extractNonFatalDiagnostics(
                        qualifiedAccess.source,
                        explicitReceiver = null,
                        it,
                        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
                        session
                    )
                )
                annotations += qualifiedAccess.annotations
            }.apply {
                setTypeOfQualifier(session)
            }
        }
    }

    return FqName.ROOT.continueQualifierInPackage(
        name,
        qualifiedAccess,
        nonFatalDiagnosticsFromExpression,
        this
    )
}

fun FirResolvedQualifier.continueQualifier(
    namedReference: FirSimpleNamedReference,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    session: FirSession,
    components: BodyResolveComponents,
): FirResolvedQualifier? {
    val name = namedReference.name
    symbol?.let { outerClassSymbol ->
        val firClass = outerClassSymbol.fir
        if (firClass !is FirClass) return null
        return firClass.scopeProvider.getNestedClassifierScope(firClass, components.session, components.scopeSession)
            ?.getSingleVisibleClassifier(session, components, name)
            ?.takeIf { it is FirClassLikeSymbol<*> }
            ?.let { nestedClassSymbol ->
                buildResolvedQualifier {
                    this.source = qualifiedAccess.source
                    packageFqName = this@continueQualifier.packageFqName
                    relativeClassFqName = this@continueQualifier.relativeClassFqName?.child(name)
                    symbol = nestedClassSymbol as FirClassLikeSymbol<*>
                    isFullyQualified = true

                    this.typeArguments.clear()
                    this.typeArguments.addAll(qualifiedAccess.typeArguments)
                    this.typeArguments.addAll(this@continueQualifier.typeArguments)
                    this.nonFatalDiagnostics.addAll(nonFatalDiagnosticsFromExpression.orEmpty())
                    this.nonFatalDiagnostics.addAll(
                        extractNonFatalDiagnostics(
                            qualifiedAccess.source,
                            explicitReceiver = null,
                            nestedClassSymbol,
                            extraNotFatalDiagnostics = this@continueQualifier.nonFatalDiagnostics,
                            session
                        )
                    )
                }.apply {
                    setTypeOfQualifier(components.session)
                }
            }
    }

    return packageFqName.continueQualifierInPackage(
        name,
        qualifiedAccess,
        nonFatalDiagnosticsFromExpression,
        components
    )
}

private fun FqName.continueQualifierInPackage(
    name: Name,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    components: BodyResolveComponents
): FirResolvedQualifier? {
    val childFqName = this.child(name)
    if (components.symbolProvider.getPackage(childFqName) != null) {
        return buildResolvedQualifier {
            this.source = qualifiedAccess.source
            packageFqName = childFqName
            this.typeArguments.addAll(qualifiedAccess.typeArguments)
            this.nonFatalDiagnostics.addAll(nonFatalDiagnosticsFromExpression.orEmpty())
            annotations += qualifiedAccess.annotations
        }.apply {
            setTypeOfQualifier(components.session)
        }
    }

    val classId = ClassId.topLevel(childFqName)
    val symbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null

    return buildResolvedQualifier {
        this.source = qualifiedAccess.source
        packageFqName = this@continueQualifierInPackage
        relativeClassFqName = classId.relativeClassName
        this.symbol = symbol
        this.typeArguments.addAll(qualifiedAccess.typeArguments)
        this.nonFatalDiagnostics.addAll(
            extractNonFatalDiagnostics(
                qualifiedAccess.source,
                explicitReceiver = null,
                symbol,
                extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
                components.session
            )
        )
        isFullyQualified = true
        annotations += qualifiedAccess.annotations
    }.apply {
        setTypeOfQualifier(components.session)
    }
}

internal fun extractNonFatalDiagnostics(
    source: KtSourceElement?,
    explicitReceiver: FirExpression?,
    symbol: FirClassLikeSymbol<*>,
    extraNotFatalDiagnostics: List<ConeDiagnostic>?,
    session: FirSession,
): List<ConeDiagnostic> {
    val prevDiagnostics = (explicitReceiver as? FirResolvedQualifier)?.nonFatalDiagnostics ?: emptyList()
    var result: MutableList<ConeDiagnostic>? = null

    val deprecation = symbol.getDeprecationForCallSite(session)
    if (deprecation != null) {
        result = mutableListOf()
        result.addAll(prevDiagnostics)
        result.add(ConeDeprecated(source, symbol, deprecation))
    }
    if (extraNotFatalDiagnostics != null && extraNotFatalDiagnostics.isNotEmpty()) {
        if (result == null) {
            result = mutableListOf()
            result.addAll(prevDiagnostics)
        }
        result.addAll(extraNotFatalDiagnostics)
    }

    return result?.toList() ?: prevDiagnostics
}
