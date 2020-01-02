/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirFile : FirPureAbstractElement(), FirAnnotationContainer, FirDeclaration {
    abstract override val source: FirSourceElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val session: FirSession
    abstract override val resolvePhase: FirResolvePhase
    abstract val imports: List<FirImport>
    abstract val declarations: List<FirDeclaration>
    abstract val name: String
    abstract val packageFqName: FqName

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitFile(this, data)
}
