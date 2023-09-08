/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

fun FirVariable.toQualifiedAccess(
    fakeSource: KtSourceElement? = source?.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess),
    typeRef: FirTypeRef = returnTypeRef
): FirQualifiedAccessExpression = buildPropertyAccessExpression {
    source = fakeSource
    calleeReference = buildResolvedNamedReference {
        source = fakeSource
        name = this@toQualifiedAccess.name
        resolvedSymbol = this@toQualifiedAccess.symbol
    }
    this.coneTypeOrNull = typeRef.coneTypeOrNull
}

fun generateTemporaryVariable(
    moduleData: FirModuleData,
    source: KtSourceElement?,
    name: Name,
    initializer: FirExpression,
    typeRef: FirTypeRef? = null,
    extractedAnnotations: Collection<FirAnnotation>? = null
): FirProperty =
    buildProperty {
        this.source = source
        this.moduleData = moduleData
        origin = FirDeclarationOrigin.Source
        returnTypeRef = typeRef ?: FirImplicitTypeRefImplWithoutSource
        this.name = name
        this.initializer = initializer
        symbol = FirPropertySymbol(name)
        isVar = false
        isLocal = true
        status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
        if (extractedAnnotations != null) {
            // LT extracts annotations ahead.
            // PSI extracts annotations on demand. Use a similar util in [PsiConversionUtils]
            annotations.addAll(extractedAnnotations)
        }
    }

fun generateExplicitReceiverTemporaryVariable(
    session: FirSession,
    expression: FirExpression,
    source: KtSourceElement?,
): FirProperty? {
    return (expression as? FirQualifiedAccessExpression)?.explicitReceiver
        ?.takeIf {
            // If a receiver x exists, write it to a temporary variable to prevent multiple calls to it.
            // Exceptions: ResolvedQualifiers, ThisReceivers, and SuperReference as they can't have side effects when called.
            it !is FirResolvedQualifier
                    && it !is FirThisReceiverExpression
                    && !(it is FirQualifiedAccessExpression && it.calleeReference is FirSuperReference)
        }
        ?.let { receiver ->
            // val <receiver> = x
            @OptIn(UnresolvedExpressionTypeAccess::class)
            generateTemporaryVariable(
                moduleData = session.moduleData,
                source = source,
                name = SpecialNames.RECEIVER,
                initializer = receiver,
                typeRef = receiver.coneTypeOrNull?.let {
                    buildResolvedTypeRef {
                        type = it
                        this.source = source
                    }
                }
            ).also { property ->
                // Change the expression from x.a to <receiver>.a
                val newReceiverAccess =
                    property.toQualifiedAccess(fakeSource = receiver.source?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement))

                if (expression.explicitReceiver == expression.dispatchReceiver) {
                    expression.replaceDispatchReceiver(newReceiverAccess)
                } else {
                    expression.replaceExtensionReceiver(newReceiverAccess)
                }
                expression.replaceExplicitReceiver(newReceiverAccess)
            }
        }
}
