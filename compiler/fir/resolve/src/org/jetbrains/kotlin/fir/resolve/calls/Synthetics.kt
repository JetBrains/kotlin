/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.isStatic
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctionsAndSelf
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name

class SyntheticPropertySymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : FirAccessorSymbol(callableId, accessorId), SyntheticSymbol

class FirSyntheticFunctionSymbol(
    callableId: CallableId
) : FirNamedFunctionSymbol(callableId), SyntheticSymbol

class FirSyntheticPropertiesScope(
    val session: FirSession,
    private val baseScope: FirTypeScope
) : FirScope() {
    private val syntheticNamesProvider = session.syntheticNamesProvider

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val getterNames = syntheticNamesProvider.possibleGetterNamesByPropertyName(name)
        for (getterName in getterNames) {
            baseScope.processFunctionsByName(getterName) {
                checkGetAndCreateSynthetic(name, getterName, it, processor)
            }
        }
    }

    private fun checkGetAndCreateSynthetic(
        propertyName: Name,
        getterName: Name,
        getterSymbol: FirFunctionSymbol<*>,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {
        val getter = getterSymbol.fir as? FirSimpleFunction ?: return

        if (getter.typeParameters.isNotEmpty()) return
        if (getter.valueParameters.isNotEmpty()) return
        if (getter.isStatic) return
        val getterReturnType = (getter.returnTypeRef as? FirResolvedTypeRef)?.type
        if ((getterReturnType as? ConeClassLikeType)?.lookupTag?.classId == StandardClassIds.Unit) return

        if (!getterSymbol.hasJavaOverridden()) return

        var matchingSetter: FirSimpleFunction? = null
        if (getterReturnType != null) {
            val setterName = syntheticNamesProvider.setterNameByGetterName(getterName)
            baseScope.processFunctionsByName(setterName, fun(setterSymbol: FirFunctionSymbol<*>) {
                if (matchingSetter != null) return
                val setter = setterSymbol.fir as? FirSimpleFunction ?: return
                val parameter = setter.valueParameters.singleOrNull() ?: return
                if (setter.typeParameters.isNotEmpty() || setter.isStatic) return
                val parameterType = (parameter.returnTypeRef as? FirResolvedTypeRef)?.type ?: return
                if (getterReturnType.withNullability(ConeNullability.NOT_NULL) != parameterType.withNullability(ConeNullability.NOT_NULL)) return
                matchingSetter = setter
            })
        }

        val property = buildSyntheticProperty {
            session = this@FirSyntheticPropertiesScope.session
            name = propertyName
            symbol = SyntheticPropertySymbol(
                accessorId = getterSymbol.callableId,
                callableId = CallableId(getterSymbol.callableId.packageName, getterSymbol.callableId.className, propertyName)
            )
            delegateGetter = getter
            delegateSetter = matchingSetter
        }
        processor(property.symbol)
    }

    private fun FirFunctionSymbol<*>.hasJavaOverridden(): Boolean {
        var result = false
        baseScope.processOverriddenFunctionsAndSelf(this) {
            if (it.unwrapSubstitutionOverrides().fir.origin == FirDeclarationOrigin.Enhancement) {
                result = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        return result
    }
}
