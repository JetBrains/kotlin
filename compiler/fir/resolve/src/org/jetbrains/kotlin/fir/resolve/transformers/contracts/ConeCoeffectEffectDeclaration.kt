/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.contract.contextual.CoeffectFamily
import org.jetbrains.kotlin.fir.contract.contextual.declaration.CoeffectActionExtractors
import org.jetbrains.kotlin.fir.contract.contextual.declaration.CoeffectActionExtractorsBuilder
import org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionVisitor
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeValueParameterReference

abstract class ConeAbstractCoeffectEffectDeclaration(
    val original: ConeEffectDeclaration,
    val actionExtractors: CoeffectActionExtractors
) : ConeEffectDeclaration() {
    val family: CoeffectFamily get() = actionExtractors.family
}

class ConeCoeffectEffectDeclaration(
    original: ConeEffectDeclaration,
    actionExtractors: CoeffectActionExtractors
) : ConeAbstractCoeffectEffectDeclaration(original, actionExtractors) {

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        original.accept(contractDescriptionVisitor, data)
}

class ConeLambdaCoeffectEffectDeclaration(
    original: ConeEffectDeclaration,
    val lambda: ConeValueParameterReference,
    actionExtractors: CoeffectActionExtractors
) : ConeAbstractCoeffectEffectDeclaration(original, actionExtractors) {

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        original.accept(contractDescriptionVisitor, data)
}

fun <E : ConeEffectDeclaration> E.toCoeffect(block: CoeffectActionExtractorsBuilder.() -> Unit): ConeCoeffectEffectDeclaration {
    val builder = CoeffectActionExtractorsBuilder()
    builder.block()
    return ConeCoeffectEffectDeclaration(this, builder.build())
}

fun <E : ConeEffectDeclaration> E.toLambdaCoeffect(
    lambda: ConeValueParameterReference,
    block: CoeffectActionExtractorsBuilder.() -> Unit
): ConeLambdaCoeffectEffectDeclaration {
    val builder = CoeffectActionExtractorsBuilder()
    builder.block()
    return ConeLambdaCoeffectEffectDeclaration(this, lambda, builder.build())
}