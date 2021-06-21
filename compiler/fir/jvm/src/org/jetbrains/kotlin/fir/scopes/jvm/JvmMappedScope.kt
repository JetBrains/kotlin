/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class JvmMappedScope(
    private val session: FirSession,
    private val firKotlinClass: FirClass<*>,
    private val firJavaClass: FirRegularClass,
    private val declaredMemberScope: FirScope,
    private val javaMappedClassUseSiteScope: FirTypeScope,
    private val signatures: Signatures
) : FirTypeScope() {
    private val functionsCache = mutableMapOf<FirNamedFunctionSymbol, FirNamedFunctionSymbol>()

    private val substitutor = ConeSubstitutorByMap(
        firJavaClass.typeParameters.zip(firKotlinClass.typeParameters).map { (javaParameter, kotlinParameter) ->
            javaParameter.symbol to ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(kotlinParameter.symbol), isNullable = false)
        }.toMap(),
        session
    )
    private val kotlinDispatchReceiverType = firKotlinClass.defaultType()

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        val visibleMethods = signatures.visibleMethodSignaturesByName[name]
            ?: return declaredMemberScope.processFunctionsByName(name, processor)

        val declared = mutableListOf<FirNamedFunctionSymbol>()
        declaredMemberScope.processFunctionsByName(name) { symbol ->
            declared += symbol
            processor(symbol)
        }

        val declaredSignatures by lazy {
            declared.mapTo(mutableSetOf()) { it.fir.computeJvmDescriptor() }
        }

        javaMappedClassUseSiteScope.processFunctionsByName(name) { symbol ->
            val newSymbol = getOrCreateSubstitutedCopy(symbol)

            val jvmSignature = newSymbol.fir.computeJvmDescriptor()
            if (jvmSignature in visibleMethods && jvmSignature !in declaredSignatures) {
                processor(newSymbol)
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        declaredMemberScope.processPropertiesByName(name, processor)
    }

    private fun getOrCreateSubstitutedCopy(symbol: FirNamedFunctionSymbol): FirNamedFunctionSymbol {
        return functionsCache.getOrPut(symbol) {
            val oldFunction = symbol.fir
            val newSymbol = FirNamedFunctionSymbol(CallableId(firKotlinClass.classId, symbol.callableId.callableName))
            FirFakeOverrideGenerator.createCopyForFirFunction(
                newSymbol,
                baseFunction = symbol.fir,
                session,
                symbol.fir.origin,
                newDispatchReceiverType = kotlinDispatchReceiverType,
                newParameterTypes = oldFunction.valueParameters.map { substitutor.substituteOrSelf(it.returnTypeRef.coneType) },
                newReturnType = substitutor.substituteOrSelf(oldFunction.returnTypeRef.coneType)
            )
            newSymbol
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ) = ProcessorAction.NONE

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        val hiddenConstructors = signatures.hiddenConstructors
        if (hiddenConstructors.isNotEmpty()) {
            javaMappedClassUseSiteScope.processDeclaredConstructors { symbol ->
                val jvmSignature = symbol.fir.computeJvmDescriptor()
                if (jvmSignature !in hiddenConstructors) {
                    processor(symbol)
                }
            }
        } else {
            javaMappedClassUseSiteScope.processDeclaredConstructors(processor)
        }

        declaredMemberScope.processDeclaredConstructors(processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NONE

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun getCallableNames(): Set<Name> {
        return declaredMemberScope.getContainingCallableNamesIfPresent() + signatures.visibleMethodSignaturesByName.keys
    }

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getContainingClassifierNamesIfPresent()
    }

    companion object {
        data class Signatures(val visibleMethodSignaturesByName: Map<Name, Set<String>>, val hiddenConstructors: Set<String>) {
            fun isEmpty() = visibleMethodSignaturesByName.isEmpty() && hiddenConstructors.isEmpty()
            fun isNotEmpty() = !isEmpty()
        }

        // NOTE: No-arg constructors
        @OptIn(ExperimentalStdlibApi::class)
        private val additionalHiddenConstructors = buildSet<String> {
            // kotlin.text.String pseudo-constructors should be used instead of java.lang.String constructors
            listOf(
                "",
                "Lkotlin/ByteArray;IILjava/nio/charset/Charset;",
                "Lkotlin/ByteArray;Ljava/nio/charset/Charset;",
                "Lkotlin/ByteArray;II",
                "Lkotlin/ByteArray;",
                "Lkotlin/CharArray;",
                "Lkotlin/CharArray;II",
                "Lkotlin/IntArray;II",
                "Ljava/lang/StringBuffer;",
                "Ljava/lang/StringBuilder;",
            ).mapTo(this) { arguments -> "java/lang/String.<init>($arguments)V" }

            listOf(
                "",
                "Ljava/lang/String;Ljava/lang/Throwable;",
                "Ljava/lang/Throwable;",
                "Ljava/lang/String;"
            ).mapTo(this) { arguments -> "java/lang/Throwable.<init>($arguments)V" }
        }

        fun prepareSignatures(klass: FirRegularClass, isMutable: Boolean): Signatures {

            val signaturePrefix = klass.symbol.classId.toString()
            val visibleMethodsByName = mutableMapOf<Name, MutableSet<String>>()
            JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES.filter { signature ->
                signature in JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES == isMutable &&
                        signature.startsWith(signaturePrefix)
            }.map { signature ->
                // +1 to delete dot before function name
                signature.substring(signaturePrefix.length + 1)
            }.forEach {
                visibleMethodsByName.getOrPut(Name.identifier(it.substringBefore("("))) { mutableSetOf() }.add(it)
            }

            val hiddenConstructors =
                (JvmBuiltInsSignatures.HIDDEN_CONSTRUCTOR_SIGNATURES + additionalHiddenConstructors)
                    .filter { it.startsWith(signaturePrefix) }
                    .mapTo(mutableSetOf()) { it.substring(signaturePrefix.length + 1) }

            return Signatures(visibleMethodsByName, hiddenConstructors)
        }
    }
}
