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
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.ConeNullability.NOT_NULL
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker

class FirSyntheticPropertySymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : FirAccessorSymbol(callableId, accessorId), SyntheticSymbol

class FirSyntheticFunctionSymbol(
    callableId: CallableId
) : FirNamedFunctionSymbol(callableId), SyntheticSymbol

class FirSyntheticPropertiesScope(
    val session: FirSession,
    private val baseScope: FirTypeScope
) : FirScope(), FirContainingNamesAwareScope {
    private val syntheticNamesProvider = session.syntheticNamesProvider

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val getterNames = syntheticNamesProvider.possibleGetterNamesByPropertyName(name)
        for (getterName in getterNames) {
            baseScope.processFunctionsByName(getterName) {
                checkGetAndCreateSynthetic(name, getterName, it, processor)
            }
        }
    }

    override fun getCallableNames(): Set<Name> = baseScope.getCallableNames().flatMapTo(hashSetOf()) { propertyName ->
        syntheticNamesProvider.possiblePropertyNamesByAccessorName(propertyName)
    }

    override fun getClassifierNames(): Set<Name> = emptySet()

    private fun checkGetAndCreateSynthetic(
        propertyName: Name,
        getterName: Name,
        getterSymbol: FirFunctionSymbol<*>,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {
        if (getterSymbol !is FirNamedFunctionSymbol) return
        val getter = getterSymbol.fir

        if (getter.typeParameters.isNotEmpty()) return
        if (getter.valueParameters.isNotEmpty()) return
        if (getter.isStatic) return
        val getterReturnType = (getter.returnTypeRef as? FirResolvedTypeRef)?.type
        if ((getterReturnType as? ConeClassLikeType)?.lookupTag?.classId == StandardClassIds.Unit) return

        if (!getterSymbol.hasJavaOverridden()) return

        var matchingSetter: FirSimpleFunction? = null
        if (getterReturnType != null) {
            val setterName = syntheticNamesProvider.setterNameByGetterName(getterName)
            if (setterName != null) {
                baseScope.processFunctionsByName(setterName, fun(setterSymbol: FirFunctionSymbol<*>) {
                    if (matchingSetter != null) return
                    val setter = setterSymbol.fir as? FirSimpleFunction ?: return
                    val parameter = setter.valueParameters.singleOrNull() ?: return
                    if (setter.typeParameters.isNotEmpty() || setter.isStatic) return
                    val parameterType = (parameter.returnTypeRef as? FirResolvedTypeRef)?.type ?: return
                    // TODO: at this moment it works for cases like
                    // class Base {
                    //     void setSomething(Object value) {}
                    // }
                    // class Derived extends Base {
                    //     String getSomething() { return ""; }
                    // }
                    // In FE 1.0, we should have also Object getSomething() in class Base for this to work
                    // I think details here are worth designing
                    if (!AbstractTypeChecker.isSubtypeOf(
                            session.typeContext,
                            getterReturnType.withNullability(NOT_NULL, session.typeContext),
                            parameterType.withNullability(NOT_NULL, session.typeContext)
                        )
                    ) {
                        return
                    }
                    matchingSetter = setter
                })
            }
        }

        val classLookupTag = getterSymbol.dispatchReceiverClassOrNull()
        val packageName = classLookupTag?.classId?.packageFqName ?: getterSymbol.callableId.packageName
        val className = classLookupTag?.classId?.relativeClassName

        val property = buildSyntheticProperty {
            declarationSiteSession = session
            name = propertyName
            symbol = FirSyntheticPropertySymbol(
                accessorId = getterSymbol.callableId,
                callableId = CallableId(packageName, className, propertyName)
            )
            delegateGetter = getter
            delegateSetter = matchingSetter
        }
        processor(property.symbol)
    }

    private fun FirNamedFunctionSymbol.hasJavaOverridden(): Boolean {
        var result = false
        baseScope.processOverriddenFunctionsAndSelf(this) {
            if (it.unwrapFakeOverrides().fir.origin == FirDeclarationOrigin.Enhancement) {
                result = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        return result
    }
}
