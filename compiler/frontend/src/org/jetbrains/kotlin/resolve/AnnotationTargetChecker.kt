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
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
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
            checkAnnotationEntry(entry, listOf(KotlinTarget.EXPRESSION), trace)
        }
        if (expression is JetFunctionLiteralExpression) {
            for (parameter in expression.getValueParameters()) {
                parameter.getTypeReference()?.let { check(it, trace) }
            }
        }
    }

    public fun possibleTargetSet(classDescriptor: ClassDescriptor): Set<KotlinTarget>? {
        val targetEntryDescriptor = classDescriptor.getAnnotations().findAnnotation(KotlinBuiltIns.FQ_NAMES.target)
                                    ?: return null
        val valueArguments = targetEntryDescriptor.getAllValueArguments()
        val valueArgument = valueArguments.entrySet().firstOrNull()?.getValue() as? ArrayValue ?: return null
        return valueArgument.value.filterIsInstance<EnumValue>().map {
            KotlinTarget.valueOrNull(it.value.getName().asString())
        }.filterNotNull().toSet()
    }

    private fun possibleTargetSet(entry: JetAnnotationEntry, trace: BindingTrace): Set<KotlinTarget> {
        val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return KotlinTarget.DEFAULT_TARGET_SET
        // For descriptor with error type, all targets are considered as possible
        if (descriptor.getType().isError()) return KotlinTarget.ALL_TARGET_SET
        val classDescriptor = TypeUtils.getClassDescriptor(descriptor.getType()) ?: return KotlinTarget.DEFAULT_TARGET_SET
        return possibleTargetSet(classDescriptor) ?: KotlinTarget.DEFAULT_TARGET_SET
    }

    private fun checkAnnotationEntry(entry: JetAnnotationEntry, actualTargets: List<KotlinTarget>, trace: BindingTrace) {
        val possibleTargets = possibleTargetSet(entry, trace)
        if (actualTargets.any { it in possibleTargets }) return
        trace.report(Errors.WRONG_ANNOTATION_TARGET.on(entry, actualTargets.firstOrNull()?.description ?: "unidentified target"))
    }

    private fun getActualTargetList(annotated: JetAnnotated, descriptor: ClassDescriptor?): List<KotlinTarget> {
        if (annotated is JetClassOrObject) {
            if (annotated is JetEnumEntry) return listOf(KotlinTarget.PROPERTY, KotlinTarget.FIELD)
            return if (descriptor?.getKind() == ClassKind.ANNOTATION_CLASS) {
                listOf(KotlinTarget.ANNOTATION_CLASS, KotlinTarget.CLASSIFIER)
            }
            else {
                listOf(KotlinTarget.CLASSIFIER)
            }
        }
        if (annotated is JetProperty) {
            return if (annotated.isLocal()) listOf(KotlinTarget.LOCAL_VARIABLE) else listOf(KotlinTarget.PROPERTY, KotlinTarget.FIELD)
        }
        if (annotated is JetParameter) {
            return if (annotated.hasValOrVar()) listOf(KotlinTarget.PROPERTY, KotlinTarget.FIELD) else listOf(KotlinTarget.VALUE_PARAMETER)
        }
        if (annotated is JetConstructor<*>) return listOf(KotlinTarget.CONSTRUCTOR)
        if (annotated is JetFunction) return listOf(KotlinTarget.FUNCTION)
        if (annotated is JetPropertyAccessor) {
            return if (annotated.isGetter()) listOf(KotlinTarget.PROPERTY_GETTER) else listOf(KotlinTarget.PROPERTY_SETTER)
        }
        if (annotated is JetPackageDirective) return listOf(KotlinTarget.PACKAGE)
        if (annotated is JetTypeReference) return listOf(KotlinTarget.TYPE)
        if (annotated is JetFile) return listOf(KotlinTarget.FILE)
        if (annotated is JetTypeParameter) return listOf(KotlinTarget.TYPE_PARAMETER)
        return listOf()
    }
}