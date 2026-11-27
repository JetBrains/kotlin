/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.signatures

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.types.AbstractKaFirSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal sealed class KaFirVariableSignature<out S : KaVariableSymbol> : KaVariableSignature<S>, FirSymbolBasedSignature {
    override val name: Name
        get() = withValidityAssertion {
            // The case where PSI is null is when calling `invoke()` on a variable with functional type, e.g. `x(1)` below:
            //
            //   fun foo(x: (item: Int) -> Unit) { x(1) }
            //   fun bar(x: Function1<@ParameterName("item") Int, Unit>) { x(1) }
            val nameCanBeDeclaredInAnnotation = symbol.psi == null

            runIf(nameCanBeDeclaredInAnnotation) { getValueFromParameterNameAnnotation() } ?: symbol.name
        }

    private fun getValueFromParameterNameAnnotation(): Name? {
        val resultingAnnotation = findParameterNameAnnotation() ?: return null
        val parameterNameArgument = resultingAnnotation.arguments
            .singleOrNull { it.name == StandardClassIds.Annotations.ParameterNames.parameterNameName }

        val constantArgumentValue = parameterNameArgument?.expression as? KaAnnotationValue.ConstantValue ?: return null

        return (constantArgumentValue.value.value as? String)?.let(Name::identifier)
    }

    private fun findParameterNameAnnotation(): KaAnnotation? {
        val allParameterNameAnnotations = returnType.annotations[StandardNames.FqNames.parameterNameClassId]
        val [explicitAnnotations, implicitAnnotations] = allParameterNameAnnotations.partition { it.psi != null }

        return if (explicitAnnotations.isNotEmpty()) {
            explicitAnnotations.first()
        } else {
            implicitAnnotations.singleOrNull()
        }
    }

    abstract override fun substitute(substitutor: KaSubstitutor): KaFirVariableSignature<S>

    override fun equals(other: Any?): Boolean = this === other ||
            other?.javaClass == javaClass &&
            (other as KaFirVariableSignature<*>).firSymbol == firSymbol

    override fun hashCode(): Int = firSymbol.hashCode()
}

internal class KaFirVariableDummySignature<out S : KaVariableSymbol>(
    override val firSymbol: FirVariableSymbol<*>,
    override val analysisSession: KaFirSession,
) : KaFirVariableSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S get() = entryPointImplementation { firSymbol.buildSymbol() as S }
    override val returnType: KaType get() = entryPointImplementation { symbol.returnType }
    override val receiverType: KaType? get() = entryPointImplementation { symbol.receiverType }
    override val contextParameters: List<KaVariableSignature<KaContextParameterSymbol>> by cached {
        firSymbol.fir.contextParameters.map { KaFirVariableDummySignature(it.symbol, analysisSession) }
    }

    override fun substitute(substitutor: KaSubstitutor): KaFirVariableSignature<S> = entryPointImplementation {
        if (substitutor is KaSubstitutor.Empty) return@entryPointImplementation this
        require(substitutor is AbstractKaFirSubstitutor<*>)

        KaFirVariableSubstitutorBasedSignature(firSymbol, analysisSession, substitutor.substitutor)
    }
}

internal class KaFirVariableSubstitutorBasedSignature<out S : KaVariableSymbol>(
    override val firSymbol: FirVariableSymbol<*>,
    override val analysisSession: KaFirSession,
    override val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : KaFirVariableSignature<S>(), SubstitutorBasedSignature {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S get() = entryPointImplementation { firSymbol.buildSymbol() as S }
    override val returnType: KaType by cached {
        val isVarargValueParameter = (firSymbol as? FirValueParameterSymbol)?.isVararg == true
        val coneType = firSymbol.resolvedReturnType.applyIf(isVarargValueParameter) { arrayElementType() ?: this }

        analysisSession.firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(coneType))
    }

    override val receiverType: KaType? by cached {
        firSymbol.resolvedReceiverType?.let { type ->
            analysisSession.firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(type))
        }
    }

    override val contextParameters: List<KaVariableSignature<KaContextParameterSymbol>> by cached {
        firSymbol.fir.contextParameters.map {
            KaFirVariableSubstitutorBasedSignature(it.symbol, analysisSession, coneSubstitutor)
        }
    }

    override fun substitute(substitutor: KaSubstitutor): KaFirVariableSignature<S> = entryPointImplementation {
        if (substitutor is KaSubstitutor.Empty) return@entryPointImplementation this
        require(substitutor is AbstractKaFirSubstitutor<*>)
        val chainedSubstitutor = ChainedSubstitutor(coneSubstitutor, substitutor.substitutor)

        KaFirVariableSubstitutorBasedSignature(firSymbol, analysisSession, chainedSubstitutor)
    }

    override fun equals(other: Any?): Boolean =
        super.equals(other) && (other as KaFirVariableSubstitutorBasedSignature<*>).coneSubstitutor == coneSubstitutor

    override fun hashCode(): Int = 31 * super.hashCode() + coneSubstitutor.hashCode()
}
