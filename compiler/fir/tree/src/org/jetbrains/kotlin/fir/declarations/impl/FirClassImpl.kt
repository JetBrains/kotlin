/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

open class FirClassImpl(
    session: FirSession,
    psi: PsiElement?,
    final override val symbol: FirClassSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    isExpect: Boolean,
    isActual: Boolean,
    final override val classKind: ClassKind,
    isInner: Boolean,
    isCompanion: Boolean,
    isData: Boolean,
    isInline: Boolean
) : FirAbstractMemberDeclaration(session, psi, name, visibility, modality, isExpect, isActual), FirRegularClass, FirModifiableClass {

    init {
        symbol.bind(this)
        status.isInner = isInner
        status.isCompanion = isCompanion
        status.isData = isData
        status.isInline = isInline
    }

    override val superTypeRefs = mutableListOf<FirTypeRef>()

    override val declarations = mutableListOf<FirDeclaration>()

    override var companionObject: FirRegularClass? = null

    fun addDeclaration(declaration: FirDeclaration) {
        declarations += declaration
        if (companionObject == null && declaration is FirRegularClass && declaration.isCompanion) {
            companionObject = declaration
        }
    }

    fun addDeclarations(declarations: Collection<FirDeclaration>) {
        declarations.forEach(this::addDeclaration)
    }

    override fun replaceSupertypes(newSupertypes: List<FirTypeRef>): FirRegularClass {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSupertypes)
        return this
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirRegularClass {
        superTypeRefs.transformInplace(transformer, data)
        val result = super<FirAbstractMemberDeclaration>.transformChildren(transformer, data) as FirRegularClass

        declarations.firstIsInstanceOrNull<FirConstructorImpl>()?.typeParameters?.transformInplace(transformer, data)
        // Transform declarations in last turn
        declarations.transformInplace(transformer, data)
        companionObject = declarations.asSequence().filterIsInstance<FirRegularClass>().firstOrNull { it.isCompanion }
        return result
    }
}
