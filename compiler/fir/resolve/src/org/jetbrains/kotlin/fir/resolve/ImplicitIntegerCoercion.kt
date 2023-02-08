/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirModuleCapability
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationClassIds
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

object ImplicitIntegerCoercionModuleCapability : FirModuleCapability<ImplicitIntegerCoercionModuleCapability>() {
    override val key: KClass<out ImplicitIntegerCoercionModuleCapability> = ImplicitIntegerCoercionModuleCapability::class
}

private val IMPLICIT_INTEGER_COERCION_ANNOTATION_CLASS_ID =
    ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("ImplicitIntegerCoercion"))

val FirCallableSymbol<*>.isMarkedWithImplicitIntegerCoercion get() =
    fir.moduleData.capabilities.contains(ImplicitIntegerCoercionModuleCapability) ||
            resolvedAnnotationClassIds.any { it == IMPLICIT_INTEGER_COERCION_ANNOTATION_CLASS_ID }

val FirCallableDeclaration.isMarkedWithImplicitIntegerCoercion get() =
    moduleData.capabilities.contains(ImplicitIntegerCoercionModuleCapability) ||
            resolvedAnnotationClassIds(symbol).any { it == IMPLICIT_INTEGER_COERCION_ANNOTATION_CLASS_ID }
