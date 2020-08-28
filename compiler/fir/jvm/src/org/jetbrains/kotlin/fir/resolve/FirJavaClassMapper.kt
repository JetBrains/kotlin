/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

class FirJavaClassMapper(private val session: FirSession) : FirPlatformClassMapper() {
    override fun getCorrespondingPlatformClass(declaration: FirClassLikeDeclaration<*>): FirRegularClass? {
        val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(declaration.symbol.classId.asSingleFqName().toUnsafe())
        return javaClassId?.let { session.firSymbolProvider.getClassLikeSymbolByFqName(it)?.fir } as? FirRegularClass
    }
}
