/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class FirVariableSymbol<E : FirVariable>(override val callableId: CallableId) : FirCallableSymbol<E>()

open class FirPropertySymbol(
    callableId: CallableId,
) : FirVariableSymbol<FirProperty>(callableId) {
    // TODO: should we use this constructor for local variables?
    constructor(name: Name) : this(CallableId(name))
}

class FirIntersectionOverridePropertySymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>
) : FirPropertySymbol(callableId), FirIntersectionCallableSymbol

class FirBackingFieldSymbol(callableId: CallableId) : FirVariableSymbol<FirProperty>(callableId)

class FirDelegateFieldSymbol(callableId: CallableId) : FirVariableSymbol<FirProperty>(callableId) {
    val delegate: FirExpression
        get() = fir.delegate!!
}

class FirFieldSymbol(callableId: CallableId) : FirVariableSymbol<FirField>(callableId)

class FirEnumEntrySymbol(callableId: CallableId) : FirVariableSymbol<FirEnumEntry>(callableId)

class FirValueParameterSymbol(name: Name) : FirVariableSymbol<FirValueParameter>(CallableId(name)) {
    val name: Name
        get() = callableId.callableName
}

class FirErrorPropertySymbol(
    val diagnostic: ConeDiagnostic
) : FirVariableSymbol<FirErrorProperty>(CallableId(FqName.ROOT, null, NAME)) {
    companion object {
        val NAME: Name = Name.special("<error property>")
    }
}
