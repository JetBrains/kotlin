/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirFileImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirRegularClassImpl
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef

fun FirTypeParameterBuilder.addDefaultBoundIfNecessary(isFlexible: Boolean = false) {
    if (bounds.isEmpty()) {
        val builtinTypes = moduleData.session.builtinTypes
        val type = if (isFlexible) {
            buildResolvedTypeRef {
                type = ConeFlexibleType(builtinTypes.anyType.type, builtinTypes.nullableAnyType.type)
            }
        } else {
            builtinTypes.nullableAnyType
        }
        bounds += type
    }
}

fun FirRegularClassBuilder.addDeclaration(declaration: FirDeclaration<*>) {
    declarations += declaration
    if (companionObject == null && declaration is FirRegularClass && declaration.isCompanion) {
        companionObject = declaration
    }
}

fun FirRegularClassBuilder.addDeclarations(declarations: Collection<FirDeclaration<*>>) {
    declarations.forEach(this::addDeclaration)
}

fun FirFile.addDeclaration(declaration: FirDeclaration<*>) {
    require(this is FirFileImpl)
    declarations += declaration
}

fun FirRegularClass.addDeclaration(declaration: FirDeclaration<*>) {
    @Suppress("LiftReturnOrAssignment")
    when (this) {
        is FirRegularClassImpl -> declarations += declaration
        else -> throw IllegalStateException()
    }
}
