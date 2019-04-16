/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberPropertyImpl
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.firSafeNullable
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

interface SyntheticSymbol : ConeSymbol

class SyntheticPropertySymbol(callableId: CallableId) : FirPropertySymbol(callableId), SyntheticSymbol

class FirSyntheticPropertiesScope(val session: FirSession, val baseScope: FirScope, val typeCalculator: ReturnTypeCalculator) : FirScope {

    val synthetic: MutableMap<ConeCallableSymbol, ConeVariableSymbol> = mutableMapOf()


    private fun checkGetAndCreateSynthetic(
        name: Name,
        symbol: ConeFunctionSymbol,
        processor: (ConeVariableSymbol) -> ProcessorAction
    ): ProcessorAction {
        val fir = symbol.firSafeNullable<FirNamedFunction>() ?: return ProcessorAction.NEXT

        if (fir.typeParameters.isNotEmpty()) return ProcessorAction.NEXT
        if (fir.valueParameters.isNotEmpty()) return ProcessorAction.NEXT

        val synthetic = SyntheticPropertySymbol(CallableId(symbol.callableId.packageName, symbol.callableId.className, name))

        val returnTypeRef = typeCalculator.tryCalculateReturnType(fir)
        FirMemberPropertyImpl(
            session,
            null,
            synthetic,
            name,
            fir.visibility,
            fir.modality,
            false,
            false,
            false,
            false,
            false,
            null,
            returnTypeRef,
            true,
            null,
            FirDefaultPropertyGetter(session, null, returnTypeRef, fir.visibility),
            FirDefaultPropertySetter(session, null, returnTypeRef, fir.visibility),
            null
        )
        return processor(synthetic)
    }

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        if (name.isSpecial) return ProcessorAction.NEXT
        if (baseScope.processFunctionsByName(Name.guessByFirstCharacter("get${name.identifier.capitalize()}")) {
                checkGetAndCreateSynthetic(name, it, processor)
            }.stop()) return ProcessorAction.STOP

        if (name.asString().startsWith("is") && baseScope.processFunctionsByName(name) {
                checkGetAndCreateSynthetic(name, it, processor)
            }.stop()) return ProcessorAction.STOP
        return super.processPropertiesByName(name, processor)
    }
}