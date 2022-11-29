/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.name.Name

interface FirTypeArgumentList {
    val source: KtSourceElement?
    val typeArguments: List<FirTypeProjection>
}

interface FirQualifierPart {
    val source: KtSourceElement?
    val name: Name
    val typeArgumentList: FirTypeArgumentList
}
