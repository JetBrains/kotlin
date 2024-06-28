/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

private object DestructuringDeclarationContainerVariableKey : FirDeclarationDataKey()
private object DestructuringDeclarationContainerVariableMarkerKey : FirDeclarationDataKey()

/*
 * For properties created from destructuring declaration entries, contains the symbol of the corresponding container variable
 *   Currently used only for script top-level destructuring declarations, and therefore only set for them too
 */
var FirProperty.destructuringDeclarationContainerVariable: FirVariableSymbol<*>? by FirDeclarationDataRegistry.data(DestructuringDeclarationContainerVariableKey)

var FirVariable.isDestructuringDeclarationContainerVariable:
        Boolean? by FirDeclarationDataRegistry.data(DestructuringDeclarationContainerVariableMarkerKey)
