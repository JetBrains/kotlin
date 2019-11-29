/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord

class SyntheticPropertySymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : FirNamedFunctionSymbol(callableId), AccessorSymbol

class FirSyntheticFunctionSymbol(
    callableId: CallableId
) : FirNamedFunctionSymbol(callableId), SyntheticSymbol

class FirSyntheticPropertiesScope(
    val session: FirSession,
    private val baseScope: FirScope
) : FirScope() {

    val synthetic: MutableMap<FirCallableSymbol<*>, FirVariableSymbol<*>> = mutableMapOf()

    private fun checkGetAndCreateSynthetic(
        name: Name,
        symbol: FirFunctionSymbol<*>,
        processor: (FirCallableSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        val fir = symbol.fir as? FirSimpleFunction ?: return ProcessorAction.NEXT

        if (fir.typeParameters.isNotEmpty()) return ProcessorAction.NEXT
        if (fir.valueParameters.isNotEmpty()) return ProcessorAction.NEXT

        val synthetic = SyntheticPropertySymbol(
            accessorId = symbol.callableId,
            callableId = CallableId(symbol.callableId.packageName, symbol.callableId.className, name)
        )
        synthetic.bind(fir)

        return processor(synthetic)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val getterNames = possibleGetterNamesByPropertyName(name)
        for (getterName in getterNames) {
            if (baseScope.processFunctionsByName(getterName) {
                    checkGetAndCreateSynthetic(name, it, processor)
                }.stop()
            ) return ProcessorAction.STOP
        }
        return ProcessorAction.NEXT
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

        private const val GETTER_PREFIX = "get"

        private const val IS_PREFIX = "is"
    }
}

