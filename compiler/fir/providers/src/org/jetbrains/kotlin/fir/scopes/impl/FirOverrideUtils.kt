/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.MemberWithBaseScope
import org.jetbrains.kotlin.fir.scopes.ProcessOverriddenWithBaseScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

fun filterOutOverriddenFunctions(extractedOverridden: Collection<MemberWithBaseScope<FirNamedFunctionSymbol>>): Collection<MemberWithBaseScope<FirNamedFunctionSymbol>> {
    return filterOutOverridden(extractedOverridden, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope)
}

fun filterOutOverriddenProperties(extractedOverridden: Collection<MemberWithBaseScope<FirPropertySymbol>>): Collection<MemberWithBaseScope<FirPropertySymbol>> {
    return filterOutOverridden(extractedOverridden, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope)
}

@OptIn(PrivateForInline::class)
inline fun <D : FirCallableSymbol<*>> filterOutOverridden(
    extractedOverridden: Collection<MemberWithBaseScope<D>>,
    processAllOverridden: ProcessOverriddenWithBaseScope<D>,
): Collection<MemberWithBaseScope<D>> {
    return extractedOverridden.filter { overridden1 ->
        extractedOverridden.none { overridden2 ->
            overridden1 !== overridden2 && overrides(
                overridden2,
                overridden1,
                processAllOverridden
            )
        }
    }
}

// Whether f overrides g
@PrivateForInline
inline fun <D : FirCallableSymbol<*>> overrides(
    f: MemberWithBaseScope<D>,
    g: MemberWithBaseScope<D>,
    processAllOverridden: ProcessOverriddenWithBaseScope<D>,
): Boolean {
    val (fMember, fScope) = f
    val (gMember) = g

    var result = false

    fScope.processAllOverridden(fMember) { overridden, _ ->
        if (overridden == gMember) {
            result = true
            ProcessorAction.STOP
        } else {
            ProcessorAction.NEXT
        }
    }

    return result
}
