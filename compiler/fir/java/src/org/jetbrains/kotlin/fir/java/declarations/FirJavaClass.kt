/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

class FirJavaClass internal constructor(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override val symbol: FirRegularClassSymbol,
    override val name: Name,
    visibility: Visibility,
    modality: Modality?,
    override val classKind: ClassKind,
    isTopLevel: Boolean,
    isStatic: Boolean,
    internal val javaTypeParameterStack: JavaTypeParameterStack,
    internal val existingNestedClassifierNames: List<Name>
) : FirPureAbstractElement(), FirRegularClass, FirModifiableClass<FirRegularClass> {
    override var status: FirDeclarationStatusImpl = FirDeclarationStatusImpl(visibility, modality)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()

    init {
        symbol.bind(this)
        status.isInner = !isTopLevel && !isStatic
        status.isCompanion = false
        status.isData = false
        status.isInline = false
    }

    override var resolvePhase: FirResolvePhase = FirResolvePhase.DECLARATIONS

    override val superTypeRefs = mutableListOf<FirTypeRef>()

    override val declarations = mutableListOf<FirDeclaration>()

    override val companionObject: FirRegularClass?
        get() = null

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        superTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaClass {
        declarations.transformInplace(transformer, data)
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        status = status.transformSingle(transformer, data)
        superTypeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaClass {
        status = status.transformSingle(transformer, data)
        return this
    }
}
