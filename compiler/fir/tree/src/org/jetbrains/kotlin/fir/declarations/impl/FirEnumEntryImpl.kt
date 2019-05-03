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
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirEnumEntryImpl(
    session: FirSession,
    psi: PsiElement?,
    symbol: FirClassSymbol,
    name: Name
) : FirClassImpl(
    session,
    psi,
    symbol,
    name,
    visibility = Visibilities.UNKNOWN,
    modality = Modality.FINAL,
    isExpect = false,
    isActual = false,
    classKind = ClassKind.ENUM_ENTRY,
    isInner = false,
    isCompanion = false,
    isData = false,
    isInline = false
), FirEnumEntry {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(session, null)

    override val arguments = mutableListOf<FirExpression>()

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirRegularClass {
        typeRef = typeRef.transformSingle(transformer, data)
        arguments.transformInplace(transformer, data)

        return super<FirClassImpl>.transformChildren(transformer, data)
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirCall {
        arguments.transformInplace(transformer, data)

        return this
    }
}