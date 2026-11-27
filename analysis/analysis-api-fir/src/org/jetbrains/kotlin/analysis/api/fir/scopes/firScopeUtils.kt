/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.name.Name


context(analysisSession: KaFirSession)
internal fun FirScope.getCallableSymbols(
    callableNames: Collection<Name>
): Sequence<KaCallableSymbol> = sequence {
    callableNames.forEach { name ->
        yieldList {
            processFunctionsByName(name) { firSymbol ->
                add(firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firSymbol))
            }
        }
        yieldList {
            processPropertiesByName(name) { firSymbol ->
                add(firSymbolBuilder.callableBuilder.buildCallableSymbol(firSymbol))
            }
        }
    }
}

context(analysisSession: KaFirSession)
internal fun FirScope.getCallableSignatures(
    callableNames: Collection<Name>
): Sequence<KaCallableSignature<*>> = sequence {
    callableNames.forEach { name ->
        yieldList {
            processFunctionsByName(name) { firSymbol ->
                add(firSymbolBuilder.functionBuilder.buildFunctionSignature(firSymbol))
            }
        }
        yieldList {
            processPropertiesByName(name) { firSymbol ->
                add(firSymbolBuilder.variableBuilder.buildVariableLikeSignature(firSymbol))
            }
        }
    }
}

context(analysisSession: KaFirSession)
internal fun FirScope.getClassifierSymbols(classLikeNames: Collection<Name>): Sequence<KaClassifierSymbol> =
    sequence {
        classLikeNames.forEach { name ->
            yieldList {
                processClassifiersByName(name) { firSymbol ->
                    add(firSymbolBuilder.classifierBuilder.buildClassifierSymbol(firSymbol))
                }
            }
        }
    }

context(analysisSession: KaFirSession)
internal fun FirScope.getConstructors(): Sequence<KaConstructorSymbol> =
    sequence {
        yieldList {
            processDeclaredConstructors { firSymbol ->
                add(firSymbolBuilder.functionBuilder.buildConstructorSymbol(firSymbol))
            }
        }
    }

private suspend inline fun <T> SequenceScope<T>.yieldList(listBuilder: MutableList<T>.() -> Unit) {
    yieldAll(buildList(listBuilder))
}