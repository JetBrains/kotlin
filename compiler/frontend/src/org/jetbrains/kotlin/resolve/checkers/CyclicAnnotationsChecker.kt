/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.CYCLE_IN_ANNOTATION_PARAMETER
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.types.UnwrappedType

object CyclicAnnotationsChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (
            declaration !is KtClass || !declaration.isAnnotation() ||
            descriptor !is ClassDescriptor || descriptor.kind != ClassKind.ANNOTATION_CLASS
        ) return

        val primaryConstructor = declaration.primaryConstructor ?: return
        val primaryConstructorDescriptor = descriptor.unsubstitutedPrimaryConstructor ?: return

        val checker = Checker(descriptor)

        for ((parameter, parameterDescriptor) in primaryConstructor.valueParameters.zip(primaryConstructorDescriptor.valueParameters)) {
            if (checker.parameterHasCycle(descriptor, parameterDescriptor)) {
                context.trace.report(CYCLE_IN_ANNOTATION_PARAMETER.on(context.languageVersionSettings, parameter))
            }
        }
    }

    private class Checker(val targetAnnotation: ClassDescriptor) {
        private val visitedAnnotationDescriptors = mutableSetOf(targetAnnotation)
        private val annotationDescriptorsWithCycle = mutableSetOf(targetAnnotation)

        fun annotationHasCycle(annotationDescriptor: ClassDescriptor): Boolean {
            val constructorDescriptor = annotationDescriptor.unsubstitutedPrimaryConstructor ?: return false

            for (parameterDescriptor in constructorDescriptor.valueParameters) {
                if (parameterHasCycle(annotationDescriptor, parameterDescriptor)) {
                    return true
                }
            }
            return false
        }

        fun parameterHasCycle(ownedAnnotation: ClassDescriptor, parameterDescriptor: ValueParameterDescriptor): Boolean {
            val returnType = parameterDescriptor.returnType?.unwrap() ?: return false
            return when {
                returnType.arguments.isNotEmpty() && !ReflectionTypes.isKClassType(returnType) -> {
                    for (argument in returnType.arguments) {
                        if (!argument.isStarProjection) {
                            if (typeHasCycle(ownedAnnotation, argument.type.unwrap())) return true
                        }
                    }
                    false
                }
                else -> typeHasCycle(ownedAnnotation, returnType)
            }
        }

        fun typeHasCycle(ownedAnnotation: ClassDescriptor, type: UnwrappedType): Boolean {
            val referencedAnnotationDescriptor = (type.constructor.declarationDescriptor as? ClassDescriptor)
                ?.takeIf { it.kind == ClassKind.ANNOTATION_CLASS }
                ?: return false
            if (!visitedAnnotationDescriptors.add(referencedAnnotationDescriptor)) {
                return (referencedAnnotationDescriptor in annotationDescriptorsWithCycle).also {
                    if (it) {
                        annotationDescriptorsWithCycle += ownedAnnotation
                    }
                }
            }
            if (referencedAnnotationDescriptor == targetAnnotation) {
                annotationDescriptorsWithCycle += ownedAnnotation
                return true
            }
            return annotationHasCycle(referencedAnnotationDescriptor)
        }
    }
}

