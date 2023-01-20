/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionalTypeKind
import org.jetbrains.kotlin.builtins.functions.FunctionalTypeKindExtractor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.name.FqName

abstract class FirFunctionalTypeKindService : FirSessionComponent {
    protected abstract val extractor: FunctionalTypeKindExtractor

    fun getKindByClassNamePrefix(packageFqName: FqName, className: String): FunctionalTypeKind? {
        return extractor.getFunctionalClassKindWithArity(packageFqName, className)?.kind
    }

    fun hasKindWithSpecificPackage(packageFqName: FqName): Boolean {
        return extractor.hasKindWithSpecificPackage(packageFqName)
    }
}

val FirSession.functionalTypeService: FirFunctionalTypeKindService by FirSession.sessionComponentAccessor()
