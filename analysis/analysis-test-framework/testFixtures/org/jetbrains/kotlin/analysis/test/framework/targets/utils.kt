/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.components.combinedMemberScope
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.CallableId

context(_: KaSession)
internal fun findMatchingCallableSymbols(callableId: CallableId, classSymbol: KaClassSymbol): List<KaCallableSymbol> {
    val declaredSymbols = classSymbol.combinedDeclaredMemberScope
        .callables(callableId.callableName)
        .toList()

    if (declaredSymbols.isNotEmpty()) {
        return declaredSymbols
    }

    // For Java getter/setter methods that are synthesized as Kotlin properties,
    // look up the synthetic property and return the corresponding accessor's underlying function.
    val callableNameAsString = callableId.callableName.asString()
    val isGetter = JvmAbi.isGetterName(callableNameAsString)
    val isSetter = JvmAbi.isSetterName(callableNameAsString)
    if (isGetter || isSetter) {
        val propertyNames = getPropertyNamesCandidatesByAccessorName(callableId.callableName)
        for (propertyName in propertyNames) {
            for (callable in classSymbol.combinedDeclaredMemberScope.callables(propertyName)) {
                val propertySymbol = callable as? KaSyntheticJavaPropertySymbol ?: continue
                val javaMethodSymbol: KaNamedFunctionSymbol? = if (isGetter) {
                    propertySymbol.javaGetterSymbol
                } else {
                    propertySymbol.javaSetterSymbol
                }
                if (javaMethodSymbol != null && javaMethodSymbol.name == callableId.callableName) {
                    return listOf(javaMethodSymbol)
                }
            }
        }
    }

    // Fake overrides are absent in the declared member scope.
    return classSymbol.combinedMemberScope
        .callables(callableId.callableName)
        .filter { it.containingDeclaration == classSymbol }
        .toList()
}
