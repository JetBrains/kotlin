/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSettings
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

class JvmMappedScope(
    private val declaredMemberScope: FirScope,
    private val javaMappedClassUseSiteScope: FirScope,
    private val whiteListSignaturesByName: Map<Name, List<String>>
) : FirScope() {

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val whiteListSignatures = whiteListSignaturesByName[name]
            ?: return declaredMemberScope.processFunctionsByName(name, processor)
        if (!javaMappedClassUseSiteScope.processFunctionsByName(name) { symbol ->
                val jvmSignature = symbol.fir.computeJvmDescriptor()
                if (jvmSignature !in whiteListSignatures) {
                    ProcessorAction.NEXT
                } else {
                    processor(symbol)
                }
            }
        ) return ProcessorAction.STOP

        return declaredMemberScope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        return declaredMemberScope.processPropertiesByName(name, processor)
    }

    override fun processClassifiersByName(name: Name, processor: (FirClassifierSymbol<*>) -> ProcessorAction): ProcessorAction {
        return declaredMemberScope.processClassifiersByName(name, processor)
    }

    companion object {
        fun prepareSignatures(klass: FirRegularClass): Map<Name, List<String>> {
            val signaturePrefix = klass.symbol.classId.toString()
            val filteredSignatures = JvmBuiltInsSettings.WHITE_LIST_METHOD_SIGNATURES.filter { signature ->
                signature.startsWith(signaturePrefix)
            }.map { signature ->
                // +1 to delete dot before function name
                signature.substring(signaturePrefix.length + 1)
            }
            return filteredSignatures.groupBy { Name.identifier(it.substringBefore("(")) }
        }
    }
}