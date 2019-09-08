/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class FirEnumEntryImpl(
    session: FirSession,
    psi: PsiElement?,
    override val symbol: FirClassSymbol,
    override val name: Name
) : FirEnumEntry(session, psi), FirModifiableClass {
    init {
        symbol.bind(this)
    }

    override var status = FirDeclarationStatusImpl(Visibilities.UNKNOWN, Modality.FINAL)

    override val classKind: ClassKind
        get() = ClassKind.ENUM_ENTRY

    override val typeParameters = mutableListOf<FirTypeParameter>()

    override val superTypeRefs = mutableListOf<FirTypeRef>()

    override val declarations = mutableListOf<FirDeclaration>()

    override val companionObject: FirRegularClass?
        get() = null

    override var typeRef: FirTypeRef = session.builtinTypes.enumType

    override val arguments = mutableListOf<FirExpression>()

    override var resolvePhase = FirResolvePhase.RAW_FIR

    override fun replaceSupertypes(newSupertypes: List<FirTypeRef>): FirRegularClass {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSupertypes)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    fun addDeclaration(declaration: FirDeclaration) {
        declarations += declaration
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirRegularClass {
        superTypeRefs.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        status = status.transformSingle(transformer, data)

        declarations.firstIsInstanceOrNull<FirConstructorImpl>()?.typeParameters?.transformInplace(transformer, data)
        // Transform declarations in last turn
        declarations.transformInplace(transformer, data)

        typeRef = typeRef.transformSingle(transformer, data)
        arguments.transformInplace(transformer, data)

        return super<FirEnumEntry>.transformChildren(transformer, data) as FirRegularClass
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirCall {
        arguments.transformInplace(transformer, data)

        return this
    }
}