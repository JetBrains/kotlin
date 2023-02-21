/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.Modality

@PrettyIrDsl
interface IrDeclarationWithModalityBuilder {

    var declarationModality: Modality

    @IrNodePropertyDsl
    fun modality(modality: Modality) {
        this.declarationModality = modality
    }

    @IrNodePropertyDsl
    fun modalityFinal() {
        modality(Modality.FINAL)
    }

    @IrNodePropertyDsl
    fun modalitySealed() {
        modality(Modality.SEALED)
    }

    @IrNodePropertyDsl
    fun modalityOpen() {
        modality(Modality.OPEN)
    }

    @IrNodePropertyDsl
    fun modalityAbstract() {
        modality(Modality.ABSTRACT)
    }
}
