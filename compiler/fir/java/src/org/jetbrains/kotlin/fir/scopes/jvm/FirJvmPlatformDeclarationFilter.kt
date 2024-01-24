/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.name.Name

internal object FirJvmPlatformDeclarationFilter {
    fun isFunctionAvailable(function: FirSimpleFunction, javaClassScope: FirTypeScope, session: FirSession): Boolean {
        // Optimization: only run the below logic for functions named "getOrDefault" and "remove", since only two functions with these names
        // in kotlin.collections.Map are currently annotated with @PlatformDependent.
        if (function.name !in namesToCheck) return true

        if (!function.hasAnnotation(StandardNames.FqNames.platformDependentClassId, session)) return true

        var isFunctionPresentInJavaAnalogue = false
        val jvmDescriptorOfKotlinFunction = function.computeJvmDescriptor()
        javaClassScope.processFunctionsByName(function.name) { javaAnalogueFunctionSymbol ->
            if (javaAnalogueFunctionSymbol.fir.computeJvmDescriptor() == jvmDescriptorOfKotlinFunction) {
                isFunctionPresentInJavaAnalogue = true
            }
        }
        return isFunctionPresentInJavaAnalogue
    }

    private val namesToCheck = listOf("getOrDefault", "remove").map(Name::identifier)
}
