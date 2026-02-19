/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds

@NoMutableState
class FirJavaClassMapper(private val session: FirSession) : FirPlatformClassMapper() {
    override fun getCorrespondingPlatformClass(declaration: FirClassLikeDeclaration): FirRegularClass? {
        val javaClassId = getCorrespondingPlatformClass(declaration.symbol.classId)
        return javaClassId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it)?.fir } as? FirRegularClass
    }

    override fun getCorrespondingPlatformClass(classId: ClassId?): ClassId? {
        if (classId == null) return null
        return JavaToKotlinClassMap.mapKotlinToJava(classId.asSingleFqName().toUnsafe())
    }

    override fun getCorrespondingKotlinClass(classId: ClassId?): ClassId? {
        if (classId == null) return null
        return JavaToKotlinClassMap.mapJavaToKotlin(classId.asSingleFqName())
    }

    override val classTypealiasesThatDontCauseAmbiguity: Map<ClassId, ClassId> = mapOf(
        JvmStandardClassIds.Annotations.Throws to JvmStandardClassIds.Annotations.ThrowsAlias,
    )
}
