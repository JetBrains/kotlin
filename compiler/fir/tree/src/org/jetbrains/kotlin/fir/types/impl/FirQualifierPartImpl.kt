/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirTypeArgumentList
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.Name

class FirTypeArgumentListImpl(override val source: FirSourceElement) : FirTypeArgumentList {
    override val typeArguments = mutableListOf<FirTypeProjection>()
}

class FirQualifierPartImpl(override val name: Name, override val typeArgumentList: FirTypeArgumentList) : FirQualifierPart