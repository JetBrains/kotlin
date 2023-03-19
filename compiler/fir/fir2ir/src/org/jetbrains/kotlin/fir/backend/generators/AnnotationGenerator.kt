/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.useSiteTargetsFromMetaAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.isSetter

/**
 * A generator that converts annotations in [FirAnnotationContainer] to annotations in [IrMutableAnnotationContainer].
 *
 * In general, annotations are bound to the target in the beginning, i.e., clearly targeted at source level. But, there are some cases that
 * need special handling: [AnnotationUseSiteTarget]. In particular, [FirProperty] contains all annotations associated with that property,
 * whose targets may vary. After all the necessary pieces of IR elements, e.g., backing field, are ready, this generator splits those
 * annotations to the specified targets.
 *
 * Note: Annotations on primary constructor properties are already split between value parameters and properties in FIR. Before this change,
 * it used to be done here.
 */
class AnnotationGenerator(private val components: Fir2IrComponents) : Fir2IrComponents by components {

    fun List<FirAnnotation>.toIrAnnotations(): List<IrConstructorCall> =
        mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }

    fun generate(irContainer: IrMutableAnnotationContainer, firContainer: FirAnnotationContainer) {
        irContainer.annotations = firContainer.annotations.toIrAnnotations()
    }

    private fun FirAnnotation.target(applicable: List<AnnotationUseSiteTarget>): AnnotationUseSiteTarget? =
        useSiteTarget ?: applicable.firstOrNull(useSiteTargetsFromMetaAnnotation(session)::contains)

    companion object {
        private val propertyTargets = listOf(AnnotationUseSiteTarget.PROPERTY, AnnotationUseSiteTarget.FIELD)
        private val delegatedPropertyTargets = propertyTargets + listOf(AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD)
    }

    fun generate(irValueParameter: IrValueParameter, firValueParameter: FirValueParameter) {
        irValueParameter.annotations += firValueParameter.annotations.toIrAnnotations()
    }

    fun generate(irProperty: IrProperty, property: FirProperty) {
        val applicableTargets = if (irProperty.isDelegated) delegatedPropertyTargets else propertyTargets
        irProperty.annotations += property.annotations
            .filter { it.target(applicableTargets) == AnnotationUseSiteTarget.PROPERTY }
            .toIrAnnotations()
    }

    fun generate(irField: IrField, property: FirProperty) {
        val irProperty = irField.correspondingPropertySymbol?.owner ?: throw AssertionError("$irField is not a property field")

        val applicableTargets = if (irProperty.isDelegated) delegatedPropertyTargets else propertyTargets
        irField.annotations += property.annotations.filter {
            val target = it.target(applicableTargets)
            target == AnnotationUseSiteTarget.FIELD || target == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
        }.toIrAnnotations()
    }

    fun generate(propertyAccessor: IrFunction, property: FirProperty) {
        assert(propertyAccessor.isPropertyAccessor) { "$propertyAccessor is not a property accessor." }
        if (propertyAccessor.isSetter) {
            propertyAccessor.annotations += property.annotations
                .filter { it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_SETTER }
                .toIrAnnotations()
            val parameter = propertyAccessor.valueParameters.last()
            parameter.annotations += property.annotations
                .filter { it.useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER }
                .toIrAnnotations()
        } else {
            propertyAccessor.annotations += property.annotations
                .filter { it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_GETTER }
                .toIrAnnotations()
        }
        propertyAccessor.extensionReceiverParameter?.let { receiver ->
            receiver.annotations += property.annotations
                .filter { it.useSiteTarget == AnnotationUseSiteTarget.RECEIVER }
                .toIrAnnotations()
        }
    }
}
