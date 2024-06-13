/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.name.Name


internal fun FirScope.getCallableSymbols(
    callableNames: Collection<Name>,
    builder: KaSymbolByFirBuilder
): Sequence<KaCallableSymbol> = sequence {
    callableNames.forEach { name ->
        yieldList {
            processFunctionsByName(name) { firSymbol ->
                add(builder.functionLikeBuilder.buildFunctionSymbol(firSymbol))
            }
        }
        yieldList {
            processPropertiesByName(name) { firSymbol ->
                add(builder.callableBuilder.buildCallableSymbol(firSymbol))
            }
        }
    }
}

internal fun FirScope.getCallableSignatures(
    callableNames: Collection<Name>,
    builder: KaSymbolByFirBuilder
): Sequence<KaCallableSignature<*>> = sequence {
    callableNames.forEach { name ->
        yieldList {
            processFunctionsByName(name) { firSymbol ->
                add(builder.functionLikeBuilder.buildFunctionLikeSignature(firSymbol))
            }
        }
        yieldList {
            processPropertiesByName(name) { firSymbol ->
                add(builder.variableLikeBuilder.buildVariableLikeSignature(firSymbol))
            }
        }
    }
}

internal fun FirScope.getClassifierSymbols(classLikeNames: Collection<Name>, builder: KaSymbolByFirBuilder): Sequence<KaClassifierSymbol> =
    sequence {
        classLikeNames.forEach { name ->
            yieldList {
                processClassifiersByName(name) { firSymbol ->
                    add(builder.classifierBuilder.buildClassifierSymbol(firSymbol))
                }
            }
        }
    }

internal fun FirScope.getConstructors(builder: KaSymbolByFirBuilder): Sequence<KaConstructorSymbol> =
    sequence {
        yieldList {
            processDeclaredConstructors { firSymbol ->
                add(builder.functionLikeBuilder.buildConstructorSymbol(firSymbol))
            }
        }
    }

private suspend inline fun <T> SequenceScope<T>.yieldList(listBuilder: MutableList<T>.() -> Unit) {
    yieldAll(buildList(listBuilder))
}