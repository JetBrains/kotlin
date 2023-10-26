/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.BirElementDynamicPropertyKey
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.builders.build
import org.jetbrains.kotlin.bir.builders.setCall
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirClassImpl
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.expressions.BirClassReference
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.impl.BirGetFieldImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirGetValueImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirReturnImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirVarargImpl
import org.jetbrains.kotlin.bir.get
import org.jetbrains.kotlin.bir.set
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.types.utils.typeWith
import org.jetbrains.kotlin.bir.types.utils.typeWithParameters
import org.jetbrains.kotlin.bir.util.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

context(JvmBirBackendContext)
class BirRepeatedAnnotationLowering : BirLoweringPhase() {
    private val KotlinRepeatableAnnotation = birBuiltIns.findClass(StandardNames.FqNames.repeatable)
    private val JavaRepeatableAnnotation = birBuiltIns.findClass(JvmAnnotationNames.REPEATABLE_ANNOTATION)!!

    private val elementsWithMultipleAnnotations = registerIndexKey<BirAnnotationContainerElement>(false) {
        it.annotations.size >= 2
    }
    private val repeatedAnnotations = registerIndexKey<BirConstructorCall>(false) {
        it.constructedClass == KotlinRepeatableAnnotation
    }

    private val repeatedAnnotationSyntheticContainerKey = BirElementDynamicPropertyKey<BirClass, BirClass>()
    private val repeatedAnnotationSyntheticContainerToken = acquireProperty(repeatedAnnotationSyntheticContainerKey)

    override fun invoke(module: BirModuleFragment) {
        compiledBir.getElementsWithIndex(elementsWithMultipleAnnotations).forEach { element ->
            transformMultipleAnnotations(element.annotations)?.let {
                element.annotations.clear()
                element.annotations += it
            }
        }

        compiledBir.getElementsWithIndex(repeatedAnnotations).forEach { annotation ->
            val annotationClass = annotation.parent as? BirClass ?: return@forEach
            if (annotationClass.kind != ClassKind.ANNOTATION_CLASS) return@forEach
            if (annotationClass.hasAnnotation(JavaRepeatableAnnotation)) return@forEach

            val repeatedAnnotationSyntheticContainer = createRepeatedAnnotationSyntheticContainer(annotationClass)
            annotationClass.declarations += repeatedAnnotationSyntheticContainer
            annotationClass[repeatedAnnotationSyntheticContainerToken] = repeatedAnnotationSyntheticContainer
        }
    }

    private fun transformMultipleAnnotations(annotations: List<BirConstructorCall>): List<BirConstructorCall>? {
        if (!generationState.classBuilderMode.generateBodies) return null

        val annotationsByClass = annotations.groupByTo(mutableMapOf()) { it.symbol.owner.constructedClass }
        if (annotationsByClass.values.none { it.size > 1 }) return null

        val result = mutableListOf<BirConstructorCall>()
        for (annotation in annotations) {
            val annotationClass = annotation.symbol.owner.constructedClass
            val grouped = annotationsByClass.remove(annotationClass) ?: continue
            if (grouped.size < 2) {
                result.add(grouped.single())
                continue
            }

            val containerClass = getOrCreateContainerClass(annotationClass)
            result.add(wrapAnnotationEntriesInContainer(annotationClass, containerClass, grouped))
        }
        return result
    }

    private fun getOrCreateContainerClass(annotationClass: BirClass): BirClass {
        val jvmRepeatable = annotationClass.getAnnotation(JavaRepeatableAnnotation)
        return if (jvmRepeatable != null) {
            val containerClassReference = jvmRepeatable.valueArguments[0]
            require(containerClassReference is BirClassReference) {
                "Repeatable annotation container value must be a class reference: $annotationClass"
            }
            (containerClassReference.symbol as? BirClassSymbol)?.owner
                ?: error("Repeatable annotation container must be a class: $annotationClass")
        } else {
            annotationClass[repeatedAnnotationSyntheticContainerToken]!!
        }
    }

    private fun wrapAnnotationEntriesInContainer(
        annotationClass: BirClass,
        containerClass: BirClass,
        entries: List<BirConstructorCall>,
    ): BirConstructorCall {
        val annotationType = annotationClass.typeWith()
        return BirConstructorCall.build {
            type = containerClass.defaultType
            symbol = containerClass.primaryConstructor!!
            valueArguments += BirVarargImpl(
                SourceSpan.UNDEFINED,
                birBuiltIns.arrayClass.typeWith(annotationType),
                annotationType,
            ).also {
                it.elements += entries
            }
        }
    }

    private fun createRepeatedAnnotationSyntheticContainer(annotationClass: BirClass): BirClassImpl {
        val containerClass = BirClass.build {
            kind = ClassKind.ANNOTATION_CLASS
            name = Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME)
            val clazz = this@build
            thisReceiver = BirValueParameter.build {
                name = SpecialNames.THIS
                origin = IrDeclarationOrigin.INSTANCE_RECEIVER
                index = UNDEFINED_PARAMETER_INDEX
                type = clazz.typeWithParameters(typeParameters)
            }
            superTypes = listOf(birBuiltIns.annotationType)
        }

        val propertyName = Name.identifier("value")
        val propertyType = birBuiltIns.arrayClass.typeWith(annotationClass.typeWith())

        containerClass.declarations += BirConstructor.build {
            isPrimary = true
            valueParameters += BirValueParameter.build {
                name = propertyName
                type = propertyType
            }
        }

        containerClass.declarations += BirProperty.build {
            name = propertyName
            val property = this@build
            backingField = BirField.build {
                name = propertyName
                type = propertyType
                correspondingPropertySymbol = property
            }
            val field = backingField!!
            getter = BirSimpleFunction.build {
                name = Name.special("<get-${property.name}>")
                correspondingPropertySymbol = property
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                returnType = field.type
                dispatchReceiverParameter = containerClass.thisReceiver!!.copyTo(this)
                val function = this@build
                body = BirBlockBody.build {
                    statements += BirReturnImpl(
                        SourceSpan.UNDEFINED, birBuiltIns.nothingType,
                        BirGetFieldImpl(
                            SourceSpan.UNDEFINED, field.type, field, null,
                            BirGetValueImpl(SourceSpan.UNDEFINED, dispatchReceiverParameter!!.type, dispatchReceiverParameter!!, null),
                            null
                        ),
                        function,
                    )
                }
            }
        }

        containerClass.annotations += annotationClass.annotations
            .filter {
                it.isAnnotationWithEqualFqName(StandardNames.FqNames.retention) ||
                        it.isAnnotationWithEqualFqName(StandardNames.FqNames.target)
            }
            .map { it.deepCopy() } +
                BirConstructorCall.build {
                    setCall(builtInSymbols.repeatableContainer.owner.constructors.single())
                }

        return containerClass
    }
}