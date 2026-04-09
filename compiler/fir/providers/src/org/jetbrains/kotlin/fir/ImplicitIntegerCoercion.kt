/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

object ImplicitIntegerCoercionModuleCapability : FirModuleCapability() {
    override val key: KClass<out FirModuleCapability> = ImplicitIntegerCoercionModuleCapability::class
}

private val implicitIntegerCoercionAnnotationClassId =
    ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("ImplicitIntegerCoercion"))

val FirBasedSymbol<*>.isMarkedWithImplicitIntegerCoercion: Boolean
    get() =
        fir.moduleData.capabilities.contains(ImplicitIntegerCoercionModuleCapability) ||
                resolvedAnnotationClassIds.any { it == implicitIntegerCoercionAnnotationClassId }

val FirDeclaration.isMarkedWithImplicitIntegerCoercion: Boolean
    get() =
        moduleData.capabilities.contains(ImplicitIntegerCoercionModuleCapability) ||
                symbol.resolvedAnnotationClassIds.any { it == implicitIntegerCoercionAnnotationClassId }

context(sessionHolder: SessionHolder)
val ConeKotlinType.isMarkedWithImplicitIntegerCoercion: Boolean
    get() = anyExpansionOfAnyType { symbol -> symbol.isMarkedWithImplicitIntegerCoercion }
