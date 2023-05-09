/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

/**
 * A generator that converts annotations in [FirAnnotationContainer] to annotations in [IrMutableAnnotationContainer].
 *
 * Annotations are bound to the target already in frontend, e.g.
 *
 * +  Annotations on primary constructor properties are already split between value parameters, properties and backing fields in FIR.</li>
 * +  Annotations on regular properties are also already split between properties and backing fields.</li>
 *
 * So this class task is only to convert FirAnnotations to IrAnnotations.
 * Some time before, it performed also annotation splitting between use-site targets.
 */
class AnnotationGenerator(private val components: Fir2IrComponents) : Fir2IrComponents by components {

    fun List<FirAnnotation>.toIrAnnotations(): List<IrConstructorCall> =
        mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }

    fun generate(irContainer: IrMutableAnnotationContainer, firContainer: FirAnnotationContainer) {
        irContainer.annotations = firContainer.annotations.toIrAnnotations()
    }

    fun generate(irValueParameter: IrValueParameter, firValueParameter: FirValueParameter) {
        irValueParameter.annotations += firValueParameter.annotations.toIrAnnotations()
    }

    fun generate(irProperty: IrProperty, property: FirProperty) {
        irProperty.annotations += property.annotations.toIrAnnotations()
    }

    fun generate(irField: IrField, backingField: FirBackingField) {
        irField.annotations += backingField.annotations.toIrAnnotations()
    }
}
