/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

internal object FirJvmPlatformDeclarationFilter {
    fun isFunctionAvailable(function: FirSimpleFunction, session: FirSession): Boolean {
        // Optimization: only run the below logic for functions named "getOrDefault" and "remove", since only two functions with these names
        // in kotlin.collections.Map are currently annotated with @PlatformDependent.
        if (function.name.asString() != "getOrDefault" && function.name.asString() != "remove") return true

        val javaAnalogueClassId =
            session.platformClassMapper.getCorrespondingPlatformClass(function.containingClassLookupTag()?.classId) ?: return true

        if (!function.hasAnnotation(StandardNames.FqNames.platformDependentClassId, session)) return true

        val javaAnalogue = session.symbolProvider.getClassLikeSymbolByClassId(javaAnalogueClassId) as? FirClassSymbol<*> ?: return true
        val scope = javaAnalogue.unsubstitutedScope(session, ScopeSession(), withForcedTypeCalculator = false, null)
        var isFunctionPresentInJavaAnalogue = false
        scope.processFunctionsByName(function.name) {
            if (it.fir.computeJvmDescriptor() == function.computeJvmDescriptor()) {
                isFunctionPresentInJavaAnalogue = true
            }
        }
        return isFunctionPresentInJavaAnalogue
    }
}
