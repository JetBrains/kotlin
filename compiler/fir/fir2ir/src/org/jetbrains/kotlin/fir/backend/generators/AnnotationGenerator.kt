/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.useSiteTargetsFromMetaAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
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
 */
class AnnotationGenerator(private val components: Fir2IrComponents) : Fir2IrComponents by components {

    fun List<FirAnnotationCall>.toIrAnnotations(): List<IrConstructorCall> =
        mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }

    fun generate(irContainer: IrMutableAnnotationContainer, firContainer: FirAnnotationContainer) {
        irContainer.annotations = firContainer.annotations.toIrAnnotations()
    }

    private fun FirAnnotationCall.target(applicable: List<AnnotationUseSiteTarget>): AnnotationUseSiteTarget? =
        useSiteTarget ?: applicable.firstOrNull(useSiteTargetsFromMetaAnnotation(session)::contains)

    companion object {
        // Priority order: constructor parameter (if applicable) -> property -> field. So, for example, if `A`
        // can be attached to all three, then in a declaration like
        //     class C(@A val x: Int) { @A val y = 1 }
        // the parameter `x` and the property `y` will have the annotation, while the property `x` and both backing fields will not.
        private val propertyTargets = listOf(AnnotationUseSiteTarget.PROPERTY, AnnotationUseSiteTarget.FIELD)
        private val constructorPropertyTargets = listOf(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER) + propertyTargets
        private val delegatedPropertyTargets = propertyTargets + listOf(AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD)
    }

    // TODO: third argument should be whether this parameter is a property declaration (though this probably makes no difference)
    fun generate(irValueParameter: IrValueParameter, firValueParameter: FirValueParameter, isInConstructor: Boolean) {
        if (isInConstructor) {
            irValueParameter.annotations += firValueParameter.annotations
                .filter { it.target(constructorPropertyTargets) == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER }
                .toIrAnnotations()
        } else {
            irValueParameter.annotations += firValueParameter.annotations.toIrAnnotations()
        }
    }

    fun generate(irProperty: IrProperty, property: FirProperty) {
        val applicableTargets = when {
            property.source?.kind == FirFakeSourceElementKind.PropertyFromParameter -> constructorPropertyTargets
            irProperty.isDelegated -> delegatedPropertyTargets
            else -> propertyTargets
        }
        irProperty.annotations += property.annotations
            .filter { it.target(applicableTargets) == AnnotationUseSiteTarget.PROPERTY }
            .toIrAnnotations()
    }

    fun generate(irField: IrField, property: FirProperty) {
        val irProperty = irField.correspondingPropertySymbol?.owner ?: throw AssertionError("$irField is not a property field")
        val applicableTargets = when {
            property.source?.kind == FirFakeSourceElementKind.PropertyFromParameter -> constructorPropertyTargets
            irProperty.isDelegated -> delegatedPropertyTargets
            else -> propertyTargets
        }
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
            val parameter = propertyAccessor.valueParameters.single()
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
