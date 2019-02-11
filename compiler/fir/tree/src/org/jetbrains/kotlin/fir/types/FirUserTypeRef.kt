/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

interface FirQualifierPart : FirTypeProjectionContainer {
    val name: Name
}

interface FirUserTypeRef : FirTypeRefWithNullability {
    val qualifier: List<FirQualifierPart>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitUserTypeRef(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super.acceptChildren(visitor, data)
        for (qualifier in qualifier.reversed()) {
            for (argument in qualifier.typeArguments) {
                argument.accept(visitor, data)
            }
        }
    }
}