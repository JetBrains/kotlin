/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import java.lang.annotation.ElementType
import java.util.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationTarget
import kotlin.annotation

public object AnnotationTargetChecker {

    public fun check(annotated: JetAnnotated, trace: BindingTrace, descriptor: ClassDescriptor? = null) {
        if (annotated is JetTypeParameter) return // TODO: support type parameter annotations
        val actualTargets = getActualTargetList(annotated, descriptor)
        for (entry in annotated.getAnnotationEntries()) {
            checkAnnotationEntry(entry, actualTargets, trace)
        }
        if (annotated is JetCallableDeclaration) {
            annotated.getTypeReference()?.let { check(it, trace) }
        }
        if (annotated is JetFunction) {
            for (parameter in annotated.getValueParameters()) {
                if (!parameter.hasValOrVar()) {
                    check(parameter, trace)
                    if (annotated is JetFunctionLiteral) {
                        parameter.getTypeReference()?.let { check(it, trace) }
                    }
                }
            }
        }
        if (annotated is JetClassOrObject) {
            for (initializer in annotated.getAnonymousInitializers()) {
                check(initializer, trace)
            }
        }
    }

    public fun checkExpression(expression: JetExpression, trace: BindingTrace) {
        for (entry in expression.getAnnotationEntries()) {
            checkAnnotationEntry(entry, listOf(AnnotationTarget.EXPRESSION), trace)
        }
        if (expression is JetFunctionLiteralExpression) {
            for (parameter in expression.getValueParameters()) {
                parameter.getTypeReference()?.let { check(it, trace) }
            }
        }
    }

    public fun possibleTargetSet(classDescriptor: ClassDescriptor): Set<AnnotationTarget>? {
        val targetEntryDescriptor = classDescriptor.getAnnotations().findAnnotation(KotlinBuiltIns.FQ_NAMES.target)
                                    ?: return null
        val valueArguments = targetEntryDescriptor.getAllValueArguments()
        val valueArgument = valueArguments.entrySet().firstOrNull()?.getValue() as? ArrayValue ?: return null
        return valueArgument.value.filterIsInstance<EnumValue>().map {
            AnnotationTarget.valueOrNull(it.value.getName().asString())
        }.filterNotNull().toSet()
    }

    private fun possibleTargetSet(entry: JetAnnotationEntry, trace: BindingTrace): Set<AnnotationTarget> {
        val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return AnnotationTarget.DEFAULT_TARGET_SET
        // For descriptor with error type, all targets are considered as possible
        if (descriptor.getType().isError()) return AnnotationTarget.ALL_TARGET_SET
        val classDescriptor = TypeUtils.getClassDescriptor(descriptor.getType()) ?: return AnnotationTarget.DEFAULT_TARGET_SET
        return possibleTargetSet(classDescriptor) ?: AnnotationTarget.DEFAULT_TARGET_SET
    }

    private fun checkAnnotationEntry(entry: JetAnnotationEntry, actualTargets: List<AnnotationTarget>, trace: BindingTrace) {
        val possibleTargets = possibleTargetSet(entry, trace)
        if (actualTargets.any { it in possibleTargets }) return
        trace.report(Errors.WRONG_ANNOTATION_TARGET.on(entry, actualTargets.firstOrNull()?.description ?: "unidentified target"))
    }

    private fun getActualTargetList(annotated: JetAnnotated, descriptor: ClassDescriptor?): List<AnnotationTarget> {
        if (annotated is JetClassOrObject) {
            if (annotated is JetEnumEntry) return listOf(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
            return if (descriptor?.getKind() == ClassKind.ANNOTATION_CLASS) {
                listOf(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASSIFIER)
            }
            else {
                listOf(AnnotationTarget.CLASSIFIER)
            }
        }
        if (annotated is JetProperty) {
            return if (annotated.isLocal()) listOf(AnnotationTarget.LOCAL_VARIABLE) else listOf(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
        }
        if (annotated is JetParameter) {
            return if (annotated.hasValOrVar()) listOf(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD) else listOf(AnnotationTarget.VALUE_PARAMETER)
        }
        if (annotated is JetConstructor<*>) return listOf(AnnotationTarget.CONSTRUCTOR)
        if (annotated is JetFunction) return listOf(AnnotationTarget.FUNCTION)
        if (annotated is JetPropertyAccessor) {
            return if (annotated.isGetter()) listOf(AnnotationTarget.PROPERTY_GETTER) else listOf(AnnotationTarget.PROPERTY_SETTER)
        }
        if (annotated is JetPackageDirective) return listOf(AnnotationTarget.PACKAGE)
        if (annotated is JetTypeReference) return listOf(AnnotationTarget.TYPE)
        if (annotated is JetFile) return listOf(AnnotationTarget.FILE)
        if (annotated is JetTypeParameter) return listOf(AnnotationTarget.TYPE_PARAMETER)
        return listOf()
    }
}