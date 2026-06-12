/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirIdeOnly
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedQualifierImpl
import org.jetbrains.kotlin.fir.resolve.FirResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.FqName

@FirBuilderDsl
class FirResolvedQualifierBuilder : FirAbstractResolvedQualifierBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var contextSensitiveAlternative: FirPropertyAccessExpression? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var packageFqName: FqName
    override var relativeClassFqName: FqName? = null
    override var qualifierSymbol: FirClassLikeSymbol<*>? = null
    override var accessedObjectSymbol: FirRegularClassSymbol? = null
    override var explicitParent: FirResolvedQualifier? = null
    override var isNullableLhsForCallableReference: Boolean = false
    override var resolvedLhsTypeForCallableReferenceOrNull: ConeKotlinType? = null
    override var resolvedToCompanionObject: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    override var resolvedSymbolOrigin: FirResolvedSymbolOrigin? = null
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()

    override fun build(): FirResolvedQualifier {
        return FirResolvedQualifierImpl(
            source,
            contextSensitiveAlternative,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            packageFqName,
            relativeClassFqName,
            qualifierSymbol,
            accessedObjectSymbol,
            explicitParent,
            isNullableLhsForCallableReference,
            resolvedLhsTypeForCallableReferenceOrNull,
            resolvedToCompanionObject,
            nonFatalDiagnostics.toMutableOrEmpty(),
            resolvedSymbolOrigin,
            typeArguments.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedQualifier(init: FirResolvedQualifierBuilder.() -> Unit): FirResolvedQualifier {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedQualifierBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class, FirIdeOnly::class, UnresolvedExpressionTypeAccess::class)
inline fun buildResolvedQualifierCopy(original: FirResolvedQualifier, init: FirResolvedQualifierBuilder.() -> Unit): FirResolvedQualifier {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirResolvedQualifierBuilder()
    copyBuilder.source = original.source
    copyBuilder.contextSensitiveAlternative = original.contextSensitiveAlternative
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.packageFqName = original.packageFqName
    copyBuilder.relativeClassFqName = original.relativeClassFqName
    copyBuilder.qualifierSymbol = original.qualifierSymbol
    copyBuilder.accessedObjectSymbol = original.accessedObjectSymbol
    copyBuilder.explicitParent = original.explicitParent
    copyBuilder.isNullableLhsForCallableReference = original.isNullableLhsForCallableReference
    copyBuilder.resolvedLhsTypeForCallableReferenceOrNull = original.resolvedLhsTypeForCallableReferenceOrNull
    copyBuilder.resolvedToCompanionObject = original.resolvedToCompanionObject
    copyBuilder.nonFatalDiagnostics.addAll(original.nonFatalDiagnostics)
    copyBuilder.resolvedSymbolOrigin = original.resolvedSymbolOrigin
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
