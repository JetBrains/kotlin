/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.FqName

interface FirFile : @VisitedSupertype FirPackageFragment, FirDeclaration, FirAnnotationContainer {
    val name: String

    val packageFqName: FqName

    val imports: List<FirImport>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        acceptAnnotations(visitor, data)
        for (import in imports) {
            import.accept(visitor, data)
        }
        super<FirPackageFragment>.acceptChildren(visitor, data)
    }
}