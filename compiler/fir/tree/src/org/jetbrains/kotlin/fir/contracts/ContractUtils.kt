/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.builder.buildEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration

val FirContractDescription.effects: List<FirEffectDeclaration>?
    get() = (this as? FirResolvedContractDescription)?.effects

fun ConeEffectDeclaration.toFirEffectDeclaration(source: FirSourceElement? = null): FirEffectDeclaration =
    buildEffectDeclaration {
        if (source != null) {
            this.source = source
        }
        effect = this@toFirEffectDeclaration
    }
