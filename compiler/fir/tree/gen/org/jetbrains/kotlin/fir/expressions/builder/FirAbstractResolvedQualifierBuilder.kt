/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
interface FirAbstractResolvedQualifierBuilder {
    abstract var source: FirSourceElement?
    abstract var typeRef: FirTypeRef
    abstract val annotations: MutableList<FirAnnotationCall>
    abstract var packageFqName: FqName
    abstract var relativeClassFqName: FqName?
    abstract var classId: ClassId?
    abstract var symbol: FirClassLikeSymbol<*>?
    abstract var isNullableLHSForCallableReference: Boolean
    abstract val typeArguments: MutableList<FirTypeProjection>

    fun build(): FirResolvedQualifier
}
