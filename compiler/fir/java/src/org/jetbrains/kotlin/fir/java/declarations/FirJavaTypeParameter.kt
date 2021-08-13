/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private class FirJavaTypeParameter @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    override val symbol: FirTypeParameterSymbol,
    override val containingDeclarationSymbol: FirBasedSymbol<*>?,
    override var bounds: List<FirTypeRef>,
    annotationBuilder: () -> List<FirAnnotationCall>
) : FirTypeParameter() {
    init {
        symbol.bind(this)
    }

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Java

    override val variance: Variance
        get() = Variance.INVARIANT

    override val isReified: Boolean
        get() = false

    override val annotations: List<FirAnnotationCall> by lazy { annotationBuilder() }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        bounds.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        bounds = bounds.map { it.transform(transformer, data) }
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceBounds(newBounds: List<FirTypeRef>) {
        bounds = newBounds
    }
}

@FirBuilderDsl
internal class FirJavaTypeParameterBuilder {
    var source: FirSourceElement? = null
    lateinit var moduleData: FirModuleData
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    lateinit var symbol: FirTypeParameterSymbol
    var containingDeclarationSymbol: FirBasedSymbol<*>? = null
    val bounds: MutableList<FirTypeRef> = mutableListOf()
    lateinit var annotationBuilder: () -> List<FirAnnotationCall>

    @OptIn(FirImplementationDetail::class)
    fun build(): FirTypeParameter {
        return FirJavaTypeParameter(
            source,
            moduleData,
            resolvePhase,
            attributes,
            name,
            symbol,
            containingDeclarationSymbol,
            bounds,
            annotationBuilder,
        )
    }

}

@OptIn(ExperimentalContracts::class)
internal inline fun buildJavaTypeParameter(init: FirJavaTypeParameterBuilder.() -> Unit): FirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirJavaTypeParameterBuilder().apply(init).build()
}
