/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirPlatformDeclarationFilter
import org.jetbrains.kotlin.fir.scopes.FirTypeScope

internal object FirJvmPlatformDeclarationFilter {
    fun isFunctionAvailable(function: FirSimpleFunction, javaClassScope: FirTypeScope, session: FirSession): Boolean {
        if (FirPlatformDeclarationFilter.isFunctionAvailable(function, session)) return true

        val jvmDescriptorOfKotlinFunction = function.computeJvmDescriptor()
        return javaClassScope.collectFunctionsByName(function.name).any { javaAnalogueFunctionSymbol ->
            javaAnalogueFunctionSymbol.fir.computeJvmDescriptor() == jvmDescriptorOfKotlinFunction
        }
    }
}
