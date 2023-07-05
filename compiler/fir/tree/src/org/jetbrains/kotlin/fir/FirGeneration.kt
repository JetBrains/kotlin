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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.Name

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
    this.typeRef = typeRef
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

