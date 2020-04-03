/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
interface FirQualifiedAccessBuilder {
    abstract var source: FirSourceElement?
    abstract val annotations: MutableList<FirAnnotationCall>
    abstract var safe: Boolean
    abstract val typeArguments: MutableList<FirTypeProjection>
    abstract var explicitReceiver: FirExpression?
    abstract var dispatchReceiver: FirExpression
    abstract var extensionReceiver: FirExpression
    fun build(): FirQualifiedAccess
}
