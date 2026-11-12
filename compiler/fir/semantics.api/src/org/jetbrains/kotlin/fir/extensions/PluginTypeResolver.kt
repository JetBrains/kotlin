/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

/**
 * Allows compiler plugins to resolve types in yet to be resolved code.
 * For example, this can be used to resolve annotation arguments or type parameter bounds
 * to aid in generating declarations with the appropriate types.
 *
 * @see typeFromQualifierParts
 */
interface PluginTypeResolver {
    /**
     * Resolves a [user type][FirUserTypeRef].
     * The resolved type may be an [error][org.jetbrains.kotlin.fir.types.FirErrorTypeRef].
     */
    fun resolveUserType(type: FirUserTypeRef): FirResolvedTypeRef
}
