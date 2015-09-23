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
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isRepeatableAnnotation
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.inline.InlineUtil

public class AnnotationChecker(private val additionalCheckers: Iterable<AdditionalAnnotationChecker>) {

    public fun check(annotated: JetAnnotated, trace: BindingTrace, descriptor: ClassDescriptor? = null) {
        if (annotated is JetTypeParameter) return // TODO: support type parameter annotations
        val actualTargets = getActualTargetList(annotated, descriptor)
        checkEntries(annotated.annotationEntries, actualTargets, trace)
        if (annotated is JetCallableDeclaration) {
            annotated.typeReference?.let { check(it, trace) }
            annotated.receiverTypeReference?.let { check(it, trace) }
        }
        if (annotated is JetDeclarationWithBody) {
            // JetFunction or JetPropertyAccessor
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
        checkEntries(expression.getAnnotationEntries(), getActualTargetList(expression, null), trace)
        if (expression is JetFunctionLiteralExpression) {
            for (parameter in expression.valueParameters) {
                parameter.typeReference?.let { check(it, trace) }
            }
        }
    }

    private fun checkEntries(entries: List<JetAnnotationEntry>, actualTargets: TargetList, trace: BindingTrace) {
        val entryTypesWithAnnotations = hashMapOf<JetType, MutableList<AnnotationUseSiteTarget?>>()

        for (entry in entries) {
            checkAnnotationEntry(entry, actualTargets, trace)
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            val classDescriptor = TypeUtils.getClassDescriptor(descriptor.type) ?: continue

            val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()
            val existingTargetsForAnnotation = entryTypesWithAnnotations.getOrPut(descriptor.type) { arrayListOf() }
            val duplicateAnnotation = useSiteTarget in existingTargetsForAnnotation
                                      || (existingTargetsForAnnotation.any { (it == null) != (useSiteTarget == null) })

            if (duplicateAnnotation && !classDescriptor.isRepeatableAnnotation()) {
                trace.report(Errors.REPEATED_ANNOTATION.on(entry));
            }

            existingTargetsForAnnotation.add(useSiteTarget)
        }
        additionalCheckers.forEach { it.checkEntries(entries, actualTargets.defaultTargets, trace) }
    }

    private fun checkAnnotationEntry(entry: JetAnnotationEntry, actualTargets: TargetList, trace: BindingTrace) {
        val applicableTargets = applicableTargetSet(entry, trace)
        val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()

        fun check(targets: List<KotlinTarget>) = targets.any {
            it in applicableTargets && (useSiteTarget == null || KotlinTarget.USE_SITE_MAPPING[useSiteTarget] == it)
        }

        fun checkUselessFunctionLiteralAnnotation() {
            // TODO: tests on different JetAnnotatedExpression (?!)
            if (KotlinTarget.FUNCTION !in applicableTargets) return
            val annotatedExpression = entry.parent as? JetAnnotatedExpression ?: return
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return
            val retention = descriptor.type.constructor.declarationDescriptor?.getAnnotationRetention()
            if (retention == KotlinRetention.SOURCE) return

            val functionLiteralExpression = annotatedExpression.baseExpression as? JetFunctionLiteralExpression ?: return
            if (InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, trace.bindingContext, false)) {
                trace.report(Errors.NON_SOURCE_ANNOTATION_ON_INLINED_FUNCTION_LITERAL.on(entry))
            }
        }

        fun applicableWithUseSiteTarget() = useSiteTarget != null && actualTargets.onlyWithUseSiteTarget.any {
            it in applicableTargets && KotlinTarget.USE_SITE_MAPPING[useSiteTarget] == it
        }

        if (check(actualTargets.defaultTargets) || check(actualTargets.canBeSubstituted) || applicableWithUseSiteTarget()) {
            checkUselessFunctionLiteralAnnotation()
            return
        }

        if (useSiteTarget != null) {
            trace.report(Errors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET.on(
                    entry, actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target", useSiteTarget.renderName))
        }
        else {
            trace.report(Errors.WRONG_ANNOTATION_TARGET.on(
                    entry, actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target"))
        }
    }

    companion object {
        private fun applicableTargetSet(entry: JetAnnotationEntry, trace: BindingTrace): Set<KotlinTarget> {
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return KotlinTarget.DEFAULT_TARGET_SET
            // For descriptor with error type, all targets are considered as possible
            if (descriptor.type.isError) return KotlinTarget.ALL_TARGET_SET
            val classDescriptor = TypeUtils.getClassDescriptor(descriptor.type) ?: return KotlinTarget.DEFAULT_TARGET_SET
            return applicableTargetSet(classDescriptor) ?: KotlinTarget.DEFAULT_TARGET_SET
        }

        @JvmStatic
        public fun applicableTargetSet(descriptor: AnnotationDescriptor): Set<KotlinTarget> {
            val classDescriptor = descriptor.type.constructor.declarationDescriptor as? ClassDescriptor ?: return emptySet()
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
            return getActualTargetList(annotated, descriptor).defaultTargets
        }

        private fun getActualTargetList(annotated: JetElement, descriptor: ClassDescriptor?): TargetList {
            return when (annotated) {
                is JetClassOrObject -> descriptor?.let { TargetList(KotlinTarget.classActualTargets(it)) } ?: TargetLists.T_CLASSIFIER
                is JetMultiDeclarationEntry -> TargetLists.T_LOCAL_VARIABLE
                is JetProperty -> {
                    if (annotated.isLocal)
                        TargetLists.T_LOCAL_VARIABLE
                    else if (annotated.parent is JetClassOrObject || annotated.parent is JetClassBody)
                        TargetLists.T_MEMBER_PROPERTY
                    else
                        TargetLists.T_TOP_LEVEL_PROPERTY
                }
                is JetParameter -> {
                    if (annotated.hasValOrVar())
                        TargetLists.T_VALUE_PARAMETER_WITH_VAL
                    else
                        TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
                }
                is JetConstructor<*> -> TargetLists.T_CONSTRUCTOR
                is JetFunction -> {
                    if (annotated.isLocal)
                        TargetLists.T_LOCAL_FUNCTION
                    else if (annotated.parent is JetClassOrObject || annotated.parent is JetClassBody)
                        TargetLists.T_MEMBER_FUNCTION
                    else
                        TargetLists.T_TOP_LEVEL_FUNCTION
                }
                is JetPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
                is JetTypeReference -> TargetLists.T_TYPE_REFERENCE
                is JetFile -> TargetLists.T_FILE
                is JetTypeParameter -> TargetLists.T_TYPE_PARAMETER
                is JetTypeProjection ->
                    if (annotated.projectionKind == JetProjectionKind.STAR) TargetLists.T_STAR_PROJECTION else TargetLists.T_TYPE_PROJECTION
                is JetClassInitializer -> TargetLists.T_INITIALIZER
                is JetMultiDeclaration -> TargetLists.T_MULTI_DECLARATION
                is JetFunctionLiteralExpression -> TargetLists.T_FUNCTION_LITERAL
                is JetObjectLiteralExpression -> TargetLists.T_OBJECT_LITERAL
                is JetExpression -> TargetLists.T_EXPRESSION
                else -> TargetLists.EMPTY
            }
        }

        private object TargetLists {
            val T_CLASSIFIER = targetList(CLASS)

            val T_LOCAL_VARIABLE = targetList(LOCAL_VARIABLE) {
                onlyWithUseSiteTarget(PROPERTY, FIELD, PROPERTY_GETTER, PROPERTY_SETTER, VALUE_PARAMETER)
            }

            val T_MULTI_DECLARATION = targetList(MULTI_DECLARATION)

            val T_MEMBER_PROPERTY = targetList(MEMBER_PROPERTY, PROPERTY) {
                extraTargets(FIELD)
                onlyWithUseSiteTarget(VALUE_PARAMETER, PROPERTY_GETTER, PROPERTY_SETTER)
            }

            val T_TOP_LEVEL_PROPERTY = targetList(TOP_LEVEL_PROPERTY, PROPERTY) {
                extraTargets(FIELD)
                onlyWithUseSiteTarget(VALUE_PARAMETER, PROPERTY_GETTER, PROPERTY_SETTER)
            }

            val T_PROPERTY_GETTER = targetList(PROPERTY_GETTER)
            val T_PROPERTY_SETTER = targetList(PROPERTY_SETTER)

            val T_VALUE_PARAMETER_WITHOUT_VAL = targetList(VALUE_PARAMETER) {
                onlyWithUseSiteTarget(PROPERTY, FIELD)
            }

            val T_VALUE_PARAMETER_WITH_VAL = targetList(VALUE_PARAMETER, PROPERTY, MEMBER_PROPERTY) {
                extraTargets(FIELD)
                onlyWithUseSiteTarget(PROPERTY_GETTER, PROPERTY_SETTER)
            }

            val T_FILE = targetList(FILE)

            val T_CONSTRUCTOR = targetList(CONSTRUCTOR)

            val T_LOCAL_FUNCTION = targetList(LOCAL_FUNCTION, FUNCTION) {
                onlyWithUseSiteTarget(VALUE_PARAMETER)
            }

            val T_MEMBER_FUNCTION = targetList(MEMBER_FUNCTION, FUNCTION) {
                onlyWithUseSiteTarget(VALUE_PARAMETER)
            }

            val T_TOP_LEVEL_FUNCTION = targetList(TOP_LEVEL_FUNCTION, FUNCTION) {
                onlyWithUseSiteTarget(VALUE_PARAMETER)
            }

            val T_EXPRESSION = targetList(EXPRESSION)

            val T_FUNCTION_LITERAL = targetList(FUNCTION_LITERAL, FUNCTION, EXPRESSION)

            val T_OBJECT_LITERAL = targetList(OBJECT_LITERAL, CLASS, EXPRESSION)

            val T_TYPE_REFERENCE = targetList(TYPE) {
                onlyWithUseSiteTarget(VALUE_PARAMETER)
            }

            val T_TYPE_PARAMETER = targetList(TYPE_PARAMETER)

            val T_STAR_PROJECTION = targetList(STAR_PROJECTION)
            val T_TYPE_PROJECTION = targetList(TYPE_PROJECTION)

            val T_INITIALIZER = targetList(INITIALIZER)


            private fun targetList(vararg target: KotlinTarget, otherTargets: TargetListBuilder.() -> Unit = {}): TargetList {
                val builder = TargetListBuilder(*target)
                builder.otherTargets()
                return builder.build()
            }

            val EMPTY = targetList()

            private class TargetListBuilder(vararg val defaultTargets: KotlinTarget) {
                private var canBeSubstituted: List<KotlinTarget> = listOf()
                private var onlyWithUseSiteTarget: List<KotlinTarget> = listOf()

                fun extraTargets(vararg targets: KotlinTarget) {
                    canBeSubstituted = targets.toList()
                }

                fun onlyWithUseSiteTarget(vararg targets: KotlinTarget) {
                    onlyWithUseSiteTarget = targets.toList()
                }

                fun build() = TargetList(defaultTargets.toList(), canBeSubstituted, onlyWithUseSiteTarget)
            }
        }

        private class TargetList(
                val defaultTargets: List<KotlinTarget>,
                val canBeSubstituted: List<KotlinTarget> = emptyList(),
                val onlyWithUseSiteTarget: List<KotlinTarget> = emptyList())
    }
}

public interface AdditionalAnnotationChecker {
    public fun checkEntries(entries: List<JetAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace)
}