/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.contextual.declaration

import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectFamily
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeValueParameterReference

abstract class ConeAbstractCoeffectEffectDeclaration(val actionExtractors: CoeffectActionExtractors) : ConeEffectDeclaration() {
    val family: CoeffectFamily get() = actionExtractors.family
}

open class ConeCoeffectEffectDeclaration(
    actionExtractors: CoeffectActionExtractors
) : ConeAbstractCoeffectEffectDeclaration(actionExtractors)

open class ConeLambdaCoeffectEffectDeclaration(
    val lambda: ConeValueParameterReference,
    actionExtractors: CoeffectActionExtractors
) : ConeAbstractCoeffectEffectDeclaration(actionExtractors)
