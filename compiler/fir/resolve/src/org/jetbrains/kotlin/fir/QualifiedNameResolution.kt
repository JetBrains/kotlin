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
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.getSingleVisibleClassifier
import org.jetbrains.kotlin.fir.resolve.createCurrentScopeList
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDeprecated
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.typeForQualifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE = "_root_ide_package_"

fun BodyResolveComponents.resolveRootPartOfQualifier(
    namedReference: FirSimpleNamedReference,
    source: KtSourceElement?,
    typeArguments: List<FirTypeProjection>,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
): FirResolvedQualifier? {
    val name = namedReference.name
    if (name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) {
        return buildResolvedQualifier {
            this.source = source
            packageFqName = FqName.ROOT
            this.nonFatalDiagnostics.addAll(nonFatalDiagnosticsFromExpression.orEmpty())
        }.apply {
            resultType = typeForQualifier(this)
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
                this.source = source
                packageFqName = classId.packageFqName
                relativeClassFqName = classId.relativeClassName
                symbol = it
                this.typeArguments.addAll(typeArguments)
                this.nonFatalDiagnostics.addAll(
                    extractNonFatalDiagnostics(
                        source,
                        explicitReceiver = null,
                        it,
                        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
                        session.languageVersionSettings.apiVersion
                    )
                )
            }.apply {
                resultType = typeForQualifier(this)
            }
        }
    }

    return FqName.ROOT.continueQualifierInPackage(
        name,
        typeArguments,
        nonFatalDiagnosticsFromExpression,
        this,
        source,
        session.languageVersionSettings.apiVersion
    )
}

fun FirResolvedQualifier.continueQualifier(
    namedReference: FirSimpleNamedReference,
    source: KtSourceElement?,
    typeArguments: List<FirTypeProjection>,
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
                    this.source = source
                    packageFqName = this@continueQualifier.packageFqName
                    relativeClassFqName = this@continueQualifier.relativeClassFqName?.child(name)
                    symbol = nestedClassSymbol as FirClassLikeSymbol<*>
                    isFullyQualified = true

                    val outerTypeArguments = this.typeArguments.toList()
                    this.typeArguments.clear()
                    this.typeArguments.addAll(typeArguments)
                    this.typeArguments.addAll(outerTypeArguments)
                    this.nonFatalDiagnostics.addAll(nonFatalDiagnosticsFromExpression.orEmpty())
                    this.nonFatalDiagnostics.addAll(
                        extractNonFatalDiagnostics(
                            source,
                            explicitReceiver = null,
                            nestedClassSymbol,
                            extraNotFatalDiagnostics = this@continueQualifier.nonFatalDiagnostics,
                            session.languageVersionSettings.apiVersion
                        )
                    )
                }.apply {
                    resultType = components.typeForQualifier(this)
                }
            }
    }

    return packageFqName.continueQualifierInPackage(
        name,
        typeArguments,
        nonFatalDiagnosticsFromExpression,
        components,
        source,
        session.languageVersionSettings.apiVersion
    )
}

private fun FqName.continueQualifierInPackage(
    name: Name,
    typeArguments: List<FirTypeProjection>,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    components: BodyResolveComponents,
    source: KtSourceElement?,
    apiVersion: ApiVersion
): FirResolvedQualifier? {
    val childFqName = this.child(name)
    if (components.symbolProvider.getPackage(childFqName) != null) {
        return buildResolvedQualifier {
            this.source = source
            packageFqName = childFqName
            this.typeArguments.addAll(typeArguments)
            this.nonFatalDiagnostics.addAll(nonFatalDiagnosticsFromExpression.orEmpty())
        }.apply {
            resultType = components.typeForQualifier(this)
        }
    }

    val classId = ClassId.topLevel(childFqName)
    val symbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null

    return buildResolvedQualifier {
        this.source = source
        packageFqName = this@continueQualifierInPackage
        relativeClassFqName = classId.relativeClassName
        this.symbol = symbol
        this.typeArguments.addAll(typeArguments)
        this.nonFatalDiagnostics.addAll(
            extractNonFatalDiagnostics(
                source,
                explicitReceiver = null,
                symbol,
                extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
                apiVersion
            )
        )
        isFullyQualified = true
    }.apply {
        resultType = components.typeForQualifier(this)
    }
}

internal fun extractNonFatalDiagnostics(
    source: KtSourceElement?,
    explicitReceiver: FirExpression?,
    symbol: FirClassLikeSymbol<*>,
    extraNotFatalDiagnostics: List<ConeDiagnostic>?,
    apiVersion: ApiVersion
): List<ConeDiagnostic> {
    val prevDiagnostics = (explicitReceiver as? FirResolvedQualifier)?.nonFatalDiagnostics ?: emptyList()
    var result: MutableList<ConeDiagnostic>? = null

    val deprecation = symbol.getDeprecation(apiVersion)?.forUseSite()
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
