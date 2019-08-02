/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberPropertyImpl
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

interface SyntheticSymbol : ConeSymbol

class SyntheticPropertySymbol(callableId: CallableId) : FirPropertySymbol(callableId), SyntheticSymbol

class FirSyntheticFunctionSymbol(
    callableId: CallableId,
    val file: FirFile
) : FirNamedFunctionSymbol(callableId), SyntheticSymbol

class FirSyntheticPropertiesScope(
    val session: FirSession,
    private val baseScope: FirScope,
    private val typeCalculator: ReturnTypeCalculator
) : FirScope() {

    val synthetic: MutableMap<ConeCallableSymbol, ConeVariableSymbol> = mutableMapOf()


    private fun checkGetAndCreateSynthetic(
        name: Name,
        symbol: FirFunctionSymbol<*>,
        processor: (FirCallableSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        val fir = symbol.fir as? FirNamedFunction ?: return ProcessorAction.NEXT

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
            isExpect = false,
            isActual = false,
            isOverride = false,
            isConst = false,
            isLateInit = false,
            receiverTypeRef = null,
            returnTypeRef = returnTypeRef,
            isVar = true,
            initializer = null,
            delegate = null
        ).apply {
            resolvePhase = fir.resolvePhase
            getter = FirDefaultPropertyGetter(this@FirSyntheticPropertiesScope.session, null, returnTypeRef, fir.visibility)
            setter = FirDefaultPropertySetter(this@FirSyntheticPropertiesScope.session, null, returnTypeRef, fir.visibility)
        }
        return processor(synthetic)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
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