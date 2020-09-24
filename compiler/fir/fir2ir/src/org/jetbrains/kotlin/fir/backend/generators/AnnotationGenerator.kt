/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isJvmFieldAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.isPropertyField
import org.jetbrains.kotlin.ir.util.isSetter

/**
 * A generator that converts annotations in [FirAnnotationContainer] to annotations in [IrMutableAnnotationContainer].
 *
 * In general, annotations are bound to the target in the beginning, i.e., clearly targeted at source level. But, there are some cases that
 * need special handling: [AnnotationUseSiteTarget]. In particular, [FirProperty] contains all annotations associated with that property,
 * whose targets may vary. After all the necessary pieces of IR elements, e.g., backing field, are ready, this generator splits those
 * annotations to the specified targets.
 */
internal class AnnotationGenerator(private val components: Fir2IrComponents) : Fir2IrComponents by components {

    fun List<FirAnnotationCall>.toIrAnnotations(): List<IrConstructorCall> =
        mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }

    fun generate(irContainer: IrMutableAnnotationContainer, firContainer: FirAnnotationContainer) {
        irContainer.annotations = firContainer.annotations.toIrAnnotations()
    }

    fun generate(irValueParameter: IrValueParameter, firValueParameter: FirValueParameter, isInConstructor: Boolean) {
        irValueParameter.annotations +=
            firValueParameter.annotations
                .filter {
                    it.useSiteTarget == null || !isInConstructor || it.useSiteTarget == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
                }
                .toIrAnnotations()
    }

    fun generate(irProperty: IrProperty, property: FirProperty) {
        irProperty.annotations +=
            property.annotations
                .filter {
                    !it.isJvmFieldAnnotation &&
                            (it.useSiteTarget == null || it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY)
                }
                .toIrAnnotations()
    }

    fun generate(irField: IrField, property: FirProperty) {
        assert(irField.isPropertyField) {
            "$irField is not a property field."
        }
        irField.annotations +=
            property.annotations
                .filter {
                    it.isJvmFieldAnnotation ||
                            it.useSiteTarget == AnnotationUseSiteTarget.FIELD ||
                            it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
                }
                .toIrAnnotations()
    }

    fun generate(propertyAccessor: IrFunction, property: FirProperty) {
        assert(propertyAccessor.isPropertyAccessor) {
            "$propertyAccessor is not a property accessor."
        }
        if (propertyAccessor.isSetter) {
            propertyAccessor.annotations +=
                property.annotations
                    .filter {
                        it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_SETTER
                    }
                    .toIrAnnotations()
            propertyAccessor.valueParameters.singleOrNull()?.annotations =
                propertyAccessor.valueParameters.singleOrNull()?.annotations?.plus(
                    property.annotations
                        .filter {
                            it.useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER
                        }
                        .toIrAnnotations()
                )!!
        } else {
            propertyAccessor.annotations +=
                property.annotations
                    .filter {
                        it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_GETTER
                    }
                    .toIrAnnotations()
        }
        propertyAccessor.extensionReceiverParameter?.annotations =
            propertyAccessor.extensionReceiverParameter?.annotations?.plus(
                property.annotations
                    .filter {
                        it.useSiteTarget == AnnotationUseSiteTarget.RECEIVER
                    }
                    .toIrAnnotations()
            )!!
    }
}
