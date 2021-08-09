/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.toConstKind
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.types.ConstantValueKind

fun FirExpression.replaceCollectionLiteralIfNeeded(expectedType: ConeKotlinType? = null): FirExpression {
    return transformSingle(CollectionLiteralReplaceTransformer, expectedType)
}

private object CollectionLiteralReplaceTransformer : FirTransformer<ConeKotlinType?>() {
    override fun <E : FirElement> transformElement(element: E, data: ConeKotlinType?): E {
        return element
    }

    override fun transformCollectionLiteral(collectionLiteral: FirCollectionLiteral, data: ConeKotlinType?): FirStatement {
        collectionLiteral.transformChildren(this, data)




        return buildQualifiedAccessExpression {

        }
    }

//    override fun <T> transformConstExpression(
//        constExpression: FirConstExpression<T>,
//        data: ConeKotlinType?
//    ): FirStatement {
//        val type = constExpression.resultType.coneTypeSafe<ConeIntegerLiteralType>() ?: return constExpression
//        val approximatedType = type.getApproximatedType(data)
//        constExpression.resultType = constExpression.resultType.resolvedTypeFromPrototype(approximatedType)
//        @Suppress("UNCHECKED_CAST")
//        val kind = approximatedType.toConstKind() as ConstantValueKind<T>
//        constExpression.replaceKind(kind)
//        return constExpression
//    }
}
