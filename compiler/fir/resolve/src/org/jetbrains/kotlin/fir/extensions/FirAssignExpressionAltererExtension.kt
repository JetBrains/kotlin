/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import kotlin.reflect.KClass

abstract class FirAssignExpressionAltererExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("AssignAlterer")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirAssignExpressionAltererExtension::class

    /**
     * At this point [variableAssignment] contains resolved and completed lhs and calleeReference(lvalue)
     *   and unresolved rValue expression
     *
     * It's allowed to transform [variableAssignment] into any kind of statement. This state should be unresolved
     *   (modulo usages of already resolved parts, like lValue). Later this statement will be resolved by compiler
     *   itself using regular resolution algorithms
     */
    abstract fun transformVariableAssignment(variableAssignment: FirVariableAssignment): FirStatement?

    fun interface Factory : FirExtension.Factory<FirAssignExpressionAltererExtension>
}

val FirExtensionService.assignAltererExtensions: List<FirAssignExpressionAltererExtension> by FirExtensionService.registeredExtensions()
