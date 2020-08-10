/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.toFirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLambdaArgumentExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirContractFunctionSymbol
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

internal fun extractEffectsFromContractFunctionCall(
    contractFunctionCall: FirFunctionCall,
    symbol: FirContractFunctionSymbol,
    transformer: FirBodyResolveTransformer,
    effectExtractor: ConeEffectExtractor
): Pair<List<FirEffectDeclaration>, List<FirStatement>> {
    val effects = mutableListOf<FirEffectDeclaration>()
    val unresolvedEffects = mutableListOf<FirStatement>()

    val contractFunction = symbol.fir
    val resolvedContractFunction = contractFunction.transformSingle(transformer, ResolutionMode.ContextIndependent)
    val contractDescription = resolvedContractFunction.contractDescription
    if (contractDescription is FirResolvedContractDescription) {
        val argumentsMapping = createArgumentsMapping(contractFunctionCall)
        if (argumentsMapping != null) {
            contractDescription.effects.forEach {
                val effect = it.substituteArguments(effectExtractor, argumentsMapping) as? FirEffectDeclaration
                if (effect == null) {
                    unresolvedEffects += contractFunctionCall
                } else {
                    effects += effect
                }
            }
        }
    }
    return effects to unresolvedEffects
}

internal fun FirEffectDeclaration.substituteArguments(
    effectExtractor: ConeEffectExtractor,
    argumentsMapping: Map<Int, FirExpression>
): FirExpression? {
    return effect.buildContractEffectFir(effectExtractor, argumentsMapping)
}


internal fun <T : FirContractDescriptionOwner> wrapEffectsInContractCall(
    firSession: FirSession,
    owner: T,
    contractDescription: FirRawContractDescription
) {
    val rawEffects = contractDescription.rawEffects
    val effectsBlock = buildAnonymousFunction {
        session = firSession
        origin = FirDeclarationOrigin.Source
        returnTypeRef = buildImplicitTypeRef()
        receiverTypeRef = buildImplicitTypeRef()
        symbol = FirAnonymousFunctionSymbol()
        isLambda = true

        body = buildBlock {
            statements += rawEffects
        }
    }

    val lambdaArgument = buildLambdaArgumentExpression {
        expression = effectsBlock
    }

    val contractCall = buildFunctionCall {
        calleeReference = buildSimpleNamedReference {
            name = Name.identifier("contract")
        }
        argumentList = buildArgumentList {
            arguments += lambdaArgument
        }
    }

    val legacyRawContractDescription = buildLegacyRawContractDescription {
        this.contractCall = contractCall
    }

    owner.replaceContractDescription(legacyRawContractDescription)
}

internal fun getResolvedSymbolIfFunctionCall(statement: FirStatement): AbstractFirBasedSymbol<*>? {
    val functionCall = statement as? FirFunctionCall
    val resolvedReference = functionCall?.calleeReference as? FirResolvedNamedReference
    return resolvedReference?.resolvedSymbol
}

internal fun <T : FirContractDescriptionOwner> obtainResolvedContractDescription(
    session: FirSession,
    transformer: FirBodyResolveTransformer,
    owner: T,
    valueParameters: List<FirValueParameter>,
    lambdaBody: FirBlock
): FirResolvedContractDescription {
    return buildResolvedContractDescription {
        val effectExtractor = ConeEffectExtractor(session, owner, valueParameters)
        for (statement in lambdaBody.statements) {
            when (val symbol = getResolvedSymbolIfFunctionCall(statement)) {
                is FirContractFunctionSymbol -> {
                    val contractFunctionCall = statement as? FirFunctionCall
                    contractFunctionCall?.let {
                        val extractedEffects = extractEffectsFromContractFunctionCall(it, symbol, transformer, effectExtractor)
                        effects += extractedEffects.first
                        unresolvedEffects += extractedEffects.second
                    }
                }
                else -> {
                    val effect = statement.accept(effectExtractor, null) as? ConeEffectDeclaration
                    if (effect == null) {
                        unresolvedEffects += statement
                    } else {
                        effects += effect.toFirEffectDeclaration(owner.source)
                    }
                }
            }
        }
    }
}