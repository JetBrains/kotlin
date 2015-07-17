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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.resolve.descriptorUtil.isRepeatableAnnotation
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*

public class AnnotationChecker(private val additionalCheckers: Iterable<AdditionalAnnotationChecker>) {

    public fun check(annotated: JetAnnotated, trace: BindingTrace, descriptor: ClassDescriptor? = null) {
        if (annotated is JetTypeParameter) return // TODO: support type parameter annotations
        val actualTargets = getActualTargetList(annotated, descriptor)
        checkEntries(annotated.annotationEntries, actualTargets, trace)
        if (annotated is JetCallableDeclaration) {
            annotated.typeReference?.let { check(it, trace) }
        }
        if (annotated is JetFunction) {
            for (parameter in annotated.valueParameters) {
                if (!parameter.hasValOrVar()) {
                    check(parameter, trace)
                    if (annotated is JetFunctionLiteral) {
                        parameter.typeReference?.let { check(it, trace) }
                    }
                }
            }
        }
    }

    public fun checkExpression(expression: JetExpression, trace: BindingTrace) {
        checkEntries(expression.getAnnotationEntries(), targetList(KotlinTarget.EXPRESSION), trace)
        if (expression is JetFunctionLiteralExpression) {
            for (parameter in expression.valueParameters) {
                parameter.typeReference?.let { check(it, trace) }
            }
        }
    }

    private fun checkEntries(entries: List<JetAnnotationEntry>, actualTargets: TargetList, trace: BindingTrace) {
        val entryTypes: MutableSet<JetType> = hashSetOf()
        for (entry in entries) {
            checkAnnotationEntry(entry, actualTargets, trace)
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            val classDescriptor = TypeUtils.getClassDescriptor(descriptor.type) ?: continue
            if (!entryTypes.add(descriptor.type) && !classDescriptor.isRepeatableAnnotation()) {
                trace.report(Errors.REPEATED_ANNOTATION.on(entry));
            }
        }
        additionalCheckers.forEach { it.checkEntries(entries, actualTargets.declarationSite, trace) }
    }

    private fun checkAnnotationEntry(entry: JetAnnotationEntry, actualTargets: TargetList, trace: BindingTrace) {
        val applicableTargets = applicableTargetSet(entry, trace)
        val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()

        if (actualTargets.declarationSite.any {
            it in applicableTargets && (useSiteTarget == null || KotlinTarget.USE_SITE_MAPPING[useSiteTarget] == it)
        }) return

        if (useSiteTarget != null && actualTargets.useSite.any {
            it in applicableTargets && KotlinTarget.USE_SITE_MAPPING[useSiteTarget] == it
        }) return

        trace.report(Errors.WRONG_ANNOTATION_TARGET.on(entry, actualTargets.declarationSite.firstOrNull()?.description ?: "unidentified target"))
    }

    companion object {

        private val PROPERTY_USE_SITE_TARGETS = listOf(
                KotlinTarget.FIELD, KotlinTarget.PROPERTY_GETTER, KotlinTarget.PROPERTY_SETTER, KotlinTarget.VALUE_PARAMETER)
        private val VALUE_PARAMETER_USE_SITE_TARGETS = PROPERTY_USE_SITE_TARGETS + KotlinTarget.PROPERTY

        private fun applicableTargetSet(entry: JetAnnotationEntry, trace: BindingTrace): Set<KotlinTarget> {
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return KotlinTarget.DEFAULT_TARGET_SET
            // For descriptor with error type, all targets are considered as possible
            if (descriptor.type.isError) return KotlinTarget.ALL_TARGET_SET
            val classDescriptor = TypeUtils.getClassDescriptor(descriptor.type) ?: return KotlinTarget.DEFAULT_TARGET_SET
            return applicableTargetSet(classDescriptor) ?: KotlinTarget.DEFAULT_TARGET_SET
        }

        public fun applicableTargetSet(classDescriptor: ClassDescriptor): Set<KotlinTarget>? {
            val targetEntryDescriptor = classDescriptor.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.target)
                                        ?: return null
            val valueArguments = targetEntryDescriptor.allValueArguments
            val valueArgument = valueArguments.entrySet().firstOrNull()?.getValue() as? ArrayValue ?: return null
            return valueArgument.value.filterIsInstance<EnumValue>().map {
                KotlinTarget.valueOrNull(it.value.name.asString())
            }.filterNotNull().toSet()
        }

        public fun getDeclarationSiteActualTargetList(annotated: JetElement, descriptor: ClassDescriptor?): List<KotlinTarget> {
            return getActualTargetList(annotated, descriptor).declarationSite
        }

        private fun getActualTargetList(annotated: JetElement, descriptor: ClassDescriptor?): TargetList {
            return when (annotated) {
                is JetClassOrObject -> descriptor?.let { TargetList(KotlinTarget.classActualTargets(it)) } ?: targetList(CLASSIFIER)
                is JetProperty ->
                    if (annotated.isLocal) {
                        extendedTargetList(PROPERTY_USE_SITE_TARGETS, LOCAL_VARIABLE)
                    }
                    else if (annotated.parent is JetClassOrObject || annotated.parent is JetClassBody) {
                        extendedTargetList(PROPERTY_USE_SITE_TARGETS, MEMBER_PROPERTY, PROPERTY, FIELD)
                    }
                    else {
                        extendedTargetList(PROPERTY_USE_SITE_TARGETS, TOP_LEVEL_PROPERTY, PROPERTY, FIELD)
                    }
                is JetParameter -> {
                    if (annotated.hasValOrVar()) {
                        extendedTargetList(VALUE_PARAMETER_USE_SITE_TARGETS, PROPERTY_PARAMETER, MEMBER_PROPERTY, PROPERTY, FIELD)
                    }
                    else {
                        extendedTargetList(VALUE_PARAMETER_USE_SITE_TARGETS, VALUE_PARAMETER)
                    }
                }
                is JetConstructor<*> -> targetList(CONSTRUCTOR)
                is JetFunction -> {
                    val extendedTargets = listOf(KotlinTarget.VALUE_PARAMETER)
                    if (annotated.isLocal) {
                        extendedTargetList(extendedTargets, LOCAL_FUNCTION, FUNCTION)
                    }
                    else if (annotated.parent is JetClassOrObject || annotated.parent is JetClassBody) {
                        extendedTargetList(extendedTargets, MEMBER_FUNCTION, FUNCTION)
                    }
                    else {
                        extendedTargetList(extendedTargets, TOP_LEVEL_FUNCTION, FUNCTION)
                    }
                }
                is JetPropertyAccessor -> if (annotated.isGetter) targetList(PROPERTY_GETTER) else targetList(PROPERTY_SETTER)
                is JetPackageDirective -> targetList(PACKAGE)
                is JetTypeReference -> targetList(TYPE)
                is JetFile -> targetList(FILE)
                is JetTypeParameter -> targetList(TYPE_PARAMETER)
                is JetTypeProjection -> {
                    if (annotated.projectionKind == JetProjectionKind.STAR) {
                        targetList(STAR_PROJECTION)
                    }
                    else {
                        targetList(TYPE_PROJECTION)
                    }
                }
                is JetClassInitializer -> targetList(INITIALIZER)
                else -> targetList()
            }
        }

        private class TargetList(val declarationSite: List<KotlinTarget>, val useSite: List<KotlinTarget> = emptyList())

        private fun targetList(vararg target: KotlinTarget): TargetList {
            return TargetList(listOf(*target), emptyList())
        }

        private fun extendedTargetList(extended: List<KotlinTarget>, vararg target: KotlinTarget): TargetList {
            return TargetList(listOf(*target), extended)
        }
    }
}

public interface AdditionalAnnotationChecker {
    public fun checkEntries(entries: List<JetAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace)
}