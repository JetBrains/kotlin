/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId

class FirClassSymbol(override val classId: ClassId) : ConeClassSymbol, AbstractFirBasedSymbol<FirRegularClass>() {
    override val kind: ClassKind
        get() = fir.classKind

    override val superTypes: List<ConeClassLikeType>
        get() = fir.superTypes.mapNotNull { it.coneTypeSafe<ConeClassLikeType>() }

    override val typeParameters: List<ConeTypeParameterSymbol>
        get() = fir.typeParameters.map { it.symbol }
}