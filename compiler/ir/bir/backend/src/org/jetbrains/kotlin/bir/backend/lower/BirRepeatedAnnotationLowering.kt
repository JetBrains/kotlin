/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.*
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirClassImpl
import org.jetbrains.kotlin.bir.expressions.BirClassReference
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.getOrPutDynamicProperty
import org.jetbrains.kotlin.bir.set
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
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
    private val KotlinRepeatableAnnotation by lz { birBuiltIns.findClass(StandardNames.FqNames.repeatable) }
    private val JavaRepeatableAnnotation by lz { birBuiltIns.findClass(JvmAnnotationNames.REPEATABLE_ANNOTATION)!! }

    private val repeatedAnnotationSyntheticContainerKey = createLocalIrProperty<_, BirClass>(BirClass)

    override fun lower(module: BirModuleFragment) {
        getAllElementsOfClass(BirClass, false).forEach { annotationClass ->
            if (annotationClass.kind == ClassKind.ANNOTATION_CLASS) {
                if (
                    KotlinRepeatableAnnotation?.let { annotationClass.hasAnnotation(it) } == true
                    && !annotationClass.hasAnnotation(JavaRepeatableAnnotation)
                ) {
                    val repeatedAnnotationSyntheticContainer = createRepeatedAnnotationSyntheticContainer(annotationClass)
                    annotationClass.declarations += repeatedAnnotationSyntheticContainer
                    annotationClass[repeatedAnnotationSyntheticContainerKey] = repeatedAnnotationSyntheticContainer
                }
            }
        }

        if (generationState.classBuilderMode.generateBodies) {
            getAllElementsOfClass(BirMutableAnnotationContainer, false).forEach { element ->
                transformMultipleAnnotations(element.annotations)?.let {
                    element.annotations = it
                }
            }
        }
    }

    private fun transformMultipleAnnotations(annotations: List<BirConstructorCall>): List<BirConstructorCall>? {
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
            annotationClass.getOrPutDynamicProperty(repeatedAnnotationSyntheticContainerKey) {
                createRepeatedAnnotationSyntheticContainer(annotationClass)
            }
        }
    }

    private fun wrapAnnotationEntriesInContainer(
        annotationClass: BirClass,
        containerClass: BirClass,
        entries: List<BirConstructorCall>,
    ): BirConstructorCall {
        val annotationType = annotationClass.symbol.typeWith()
        return birBodyScope {
            birCall(containerClass.primaryConstructor!!) {
                valueArguments[0] = birVararg(annotationType) {
                    elements += entries
                }
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
        val propertyType = birBuiltIns.arrayClass.typeWith(annotationClass.symbol.typeWith())

        containerClass.declarations += BirConstructor.build {
            isPrimary = true
            returnType = containerClass.defaultType
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
                body = birBodyScope {
                    returnTarget = this@build
                    birBlockBody {
                        +birReturn(birGetField(birGet(dispatchReceiverParameter!!), field))
                    }
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