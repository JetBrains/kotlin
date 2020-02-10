/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.isStatic
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord

class SyntheticPropertySymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : FirAccessorSymbol(callableId, accessorId), SyntheticSymbol

class FirSyntheticFunctionSymbol(
    callableId: CallableId
) : FirNamedFunctionSymbol(callableId), SyntheticSymbol

class FirSyntheticPropertiesScope(
    val session: FirSession,
    private val baseScope: FirScope
) : FirScope() {

    val synthetic: MutableMap<FirCallableSymbol<*>, FirVariableSymbol<*>> = mutableMapOf()

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

        var matchingSetter: FirSimpleFunction? = null
        if (getterReturnType != null) {
            val setterName = setterNameByGetterName(getterName)
            baseScope.processFunctionsByName(setterName, fun(setterSymbol: FirFunctionSymbol<*>) {
                if (matchingSetter != null) return
                val setter = setterSymbol.fir as? FirSimpleFunction ?: return
                val parameter = setter.valueParameters.singleOrNull() ?: return
                if (setter.typeParameters.isNotEmpty() || setter.isStatic) return
                val parameterType = (parameter.returnTypeRef as? FirResolvedTypeRef)?.type ?: return
                if (parameterType != getterReturnType) return
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

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val getterNames = possibleGetterNamesByPropertyName(name)
        for (getterName in getterNames) {
            baseScope.processFunctionsByName(getterName) {
                checkGetAndCreateSynthetic(name, getterName, it, processor)
            }
        }
    }

    companion object {
        fun possibleGetterNamesByPropertyName(name: Name): List<Name> {
            if (name.isSpecial) return emptyList()
            val identifier = name.identifier
            val capitalizedAsciiName = identifier.capitalizeAsciiOnly()
            val capitalizedFirstWordName = identifier.capitalizeFirstWord(asciiOnly = true)
            return listOfNotNull(
                Name.identifier(GETTER_PREFIX + capitalizedAsciiName),
                if (capitalizedFirstWordName == capitalizedAsciiName) null else Name.identifier(GETTER_PREFIX + capitalizedFirstWordName),
                name.takeIf { identifier.startsWith(IS_PREFIX) }
            ).filter {
                propertyNameByGetMethodName(it) == name
            }
        }

        fun setterNameByGetterName(name: Name): Name {
            val identifier = name.identifier
            val prefix = when {
                identifier.startsWith("get") -> "get"
                identifier.startsWith("is") -> "is"
                else -> throw IllegalArgumentException()
            }
            return Name.identifier("set" + identifier.removePrefix(prefix))
        }

        private const val GETTER_PREFIX = "get"

        private const val IS_PREFIX = "is"
    }
}

