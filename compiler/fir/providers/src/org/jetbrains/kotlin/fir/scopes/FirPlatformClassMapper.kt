/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.name.ClassId

abstract class FirPlatformClassMapper : FirSessionComponent {
    @NoMutableState
    object Default : FirPlatformClassMapper() {
        override fun getCorrespondingPlatformClass(declaration: FirClassLikeDeclaration): FirRegularClass? {
            return null
        }

        override fun getCorrespondingPlatformClass(classId: ClassId?): ClassId? {
            return null
        }

        override fun getCorrespondingKotlinClass(classId: ClassId?): ClassId? {
            return null
        }

        override val classTypealiasesThatDontCauseAmbiguity: Map<ClassId, ClassId> = emptyMap()
    }

    abstract fun getCorrespondingPlatformClass(declaration: FirClassLikeDeclaration): FirRegularClass?

    abstract fun getCorrespondingPlatformClass(classId: ClassId?): ClassId?

    abstract fun getCorrespondingKotlinClass(classId: ClassId?): ClassId?

    // Compiler should not report ambiguity for certain platform classes and their type aliases
    // For instance, there is no ambiguity between `kotlin.Throws` and `kotlin.jvm.Throws`
    // To achieve this goal, `FirTypeResolver` uses this map and tries to find candidates with identifiers from the map's keys
    // And remove corresponding type aliases (value of map) if they are presented in the candidate set
    abstract val classTypealiasesThatDontCauseAmbiguity: Map<ClassId, ClassId>
}

val FirSession.platformClassMapper: FirPlatformClassMapper by FirSession.sessionComponentAccessor()
