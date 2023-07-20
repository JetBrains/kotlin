/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

class FirTypeSpecificityComparatorProvider(val typeSpecificityComparator: TypeSpecificityComparator) : FirSessionComponent

val FirSession.typeSpecificityComparatorProvider: FirTypeSpecificityComparatorProvider? by FirSession.nullableSessionComponentAccessor()
