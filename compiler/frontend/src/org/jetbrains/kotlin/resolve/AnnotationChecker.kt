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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.descriptorUtil.isRepeatableAnnotation
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.isError

class AnnotationChecker(private val additionalCheckers: Iterable<AdditionalAnnotationChecker>) {

    fun check(annotated: KtAnnotated, trace: BindingTrace, descriptor: DeclarationDescriptor? = null) {
        val actualTargets = getActualTargetList(annotated, descriptor, trace)
        checkEntries(annotated.annotationEntries, actualTargets, trace, annotated)
        if (annotated is KtCallableDeclaration) {
            annotated.typeReference?.let { check(it, trace) }
            annotated.receiverTypeReference?.let { check(it, trace) }
        }
        if (annotated is KtTypeAlias) {
            annotated.getTypeReference()?.let { check(it, trace) }
        }
        if (annotated is KtTypeParameterListOwner && annotated is KtCallableDeclaration) {
            // TODO: support type parameter annotations for type parameters on classes and properties
            annotated.typeParameters.forEach { check(it, trace) }
        }
        if (annotated is KtTypeReference) {
            annotated.typeElement?.typeArgumentsAsTypes?.filterNotNull()?.forEach { check(it, trace) }
        }
        if (annotated is KtDeclarationWithBody) {
            // JetFunction or JetPropertyAccessor
            for (parameter in annotated.valueParameters) {
                if (!parameter.hasValOrVar()) {
                    check(parameter, trace)
                    if (annotated is KtFunctionLiteral) {
                        parameter.typeReference?.let { check(it, trace) }
                    }
                }
            }
        }
    }

    fun checkExpression(expression: KtExpression, trace: BindingTrace) {
        checkEntries(expression.getAnnotationEntries(), getActualTargetList(expression, null, trace), trace)
        if (expression is KtLambdaExpression) {
            for (parameter in expression.valueParameters) {
                parameter.typeReference?.let { check(it, trace) }
            }
        }
    }

    private fun KtAnnotated?.getImplicitUseSiteTargetList(): List<AnnotationUseSiteTarget> = when (this) {
        is KtParameter ->
                if (ownerFunction is KtPrimaryConstructor) UseSiteTargetsList.T_CONSTRUCTOR_PARAMETER else emptyList()
        is KtProperty ->
                if (!isLocal) UseSiteTargetsList.T_PROPERTY else emptyList()
        is KtPropertyAccessor ->
                if (isGetter) listOf(AnnotationUseSiteTarget.PROPERTY_GETTER) else listOf(AnnotationUseSiteTarget.PROPERTY_SETTER)
        else ->
                emptyList()
    }

    private fun KtAnnotated?.getDefaultUseSiteTarget(descriptor: AnnotationDescriptor) =
            getImplicitUseSiteTargetList().firstOrNull {
                KotlinTarget.USE_SITE_MAPPING[it] in AnnotationChecker.applicableTargetSet(descriptor)
            }

    private fun checkEntries(
            entries: List<KtAnnotationEntry>,
            actualTargets: TargetList,
            trace: BindingTrace,
            annotated: KtAnnotated? = null
    ) {
        val entryTypesWithAnnotations = hashMapOf<KotlinType, MutableList<AnnotationUseSiteTarget?>>()

        for (entry in entries) {
            checkAnnotationEntry(entry, actualTargets, trace)
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            val classDescriptor = descriptor.annotationClass ?: continue

            val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget() ?: annotated.getDefaultUseSiteTarget(descriptor)
            val existingTargetsForAnnotation = entryTypesWithAnnotations.getOrPut(descriptor.type) { arrayListOf() }
            val duplicateAnnotation = useSiteTarget in existingTargetsForAnnotation
                                      || (existingTargetsForAnnotation.any { (it == null) != (useSiteTarget == null) })

            if (duplicateAnnotation && !classDescriptor.isRepeatableAnnotation()) {
                trace.report(Errors.REPEATED_ANNOTATION.on(entry))
            }

            existingTargetsForAnnotation.add(useSiteTarget)
        }
        additionalCheckers.forEach { it.checkEntries(entries, actualTargets.defaultTargets, trace) }
    }

    private fun checkAnnotationEntry(entry: KtAnnotationEntry, actualTargets: TargetList, trace: BindingTrace) {
        val applicableTargets = applicableTargetSet(entry, trace)
        val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()

        fun check(targets: List<KotlinTarget>) = targets.any {
            it in applicableTargets && (useSiteTarget == null || KotlinTarget.USE_SITE_MAPPING[useSiteTarget] == it)
        }

        fun checkUselessFunctionLiteralAnnotation() {
            // TODO: tests on different JetAnnotatedExpression (?!)
            if (KotlinTarget.FUNCTION !in applicableTargets) return
            val annotatedExpression = entry.parent as? KtAnnotatedExpression ?: return
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return
            val retention = descriptor.annotationClass?.getAnnotationRetention()
            if (retention == KotlinRetention.SOURCE) return

            val functionLiteralExpression = annotatedExpression.baseExpression as? KtLambdaExpression ?: return
            if (InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, trace.bindingContext, false)) {
                trace.report(Errors.NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION.on(entry))
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
        private fun applicableTargetSet(entry: KtAnnotationEntry, trace: BindingTrace): Set<KotlinTarget> {
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return KotlinTarget.DEFAULT_TARGET_SET
            // For descriptor with error type, all targets are considered as possible
            if (descriptor.type.isError) return KotlinTarget.ALL_TARGET_SET
            return descriptor.annotationClass?.let(this::applicableTargetSet) ?: KotlinTarget.DEFAULT_TARGET_SET
        }

        @JvmStatic fun applicableTargetSet(descriptor: AnnotationDescriptor): Set<KotlinTarget> {
            val classDescriptor = descriptor.annotationClass ?: return emptySet()
            return applicableTargetSet(classDescriptor) ?: KotlinTarget.DEFAULT_TARGET_SET
        }

        fun applicableTargetSet(classDescriptor: ClassDescriptor): Set<KotlinTarget>? {
            val targetEntryDescriptor = classDescriptor.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.target)
                                        ?: return null
            val valueArguments = targetEntryDescriptor.allValueArguments
            val valueArgument = valueArguments.entries.firstOrNull()?.value as? ArrayValue ?: return null
            return valueArgument.value.filterIsInstance<EnumValue>().mapNotNull {
                KotlinTarget.valueOrNull(it.value.name.asString())
            }.toSet()
        }

        fun getDeclarationSiteActualTargetList(annotated: KtElement, descriptor: ClassDescriptor?, trace: BindingTrace):
                List<KotlinTarget> {
            return getActualTargetList(annotated, descriptor, trace).defaultTargets
        }

        private fun DeclarationDescriptor?.hasBackingField(bindingTrace: BindingTrace)
                = (this as? PropertyDescriptor)?.let { bindingTrace.get(BindingContext.BACKING_FIELD_REQUIRED, it) } ?: false

        private fun getActualTargetList(annotated: KtElement, descriptor: DeclarationDescriptor?, trace: BindingTrace): TargetList {
            return when (annotated) {
                is KtClassOrObject ->
                    (descriptor as? ClassDescriptor)?.let { TargetList(KotlinTarget.classActualTargets(it)) } ?: TargetLists.T_CLASSIFIER
                is KtDestructuringDeclarationEntry -> TargetLists.T_LOCAL_VARIABLE
                is KtProperty -> {
                    when {
                        annotated.isLocal -> TargetLists.T_LOCAL_VARIABLE
                        annotated.isMember -> TargetLists.T_MEMBER_PROPERTY(descriptor.hasBackingField(trace), annotated.hasDelegate())
                        else -> TargetLists.T_TOP_LEVEL_PROPERTY(descriptor.hasBackingField(trace), annotated.hasDelegate())
                    }
                }
                is KtParameter -> {
                    val destructuringDeclaration = annotated.destructuringDeclaration
                    when {
                        destructuringDeclaration != null -> TargetLists.T_DESTRUCTURING_DECLARATION
                        annotated.hasValOrVar() -> TargetLists.T_VALUE_PARAMETER_WITH_VAL
                        else -> TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
                    }
                }
                is KtConstructor<*> -> TargetLists.T_CONSTRUCTOR
                is KtFunction -> {
                    when {
                        ExpressionTypingUtils.isFunctionExpression(descriptor) -> TargetLists.T_FUNCTION_EXPRESSION
                        annotated.isLocal -> TargetLists.T_LOCAL_FUNCTION
                        annotated.parent is KtClassOrObject || annotated.parent is KtClassBody -> TargetLists.T_MEMBER_FUNCTION
                        else -> TargetLists.T_TOP_LEVEL_FUNCTION
                    }
                }
                is KtTypeAlias -> TargetLists.T_TYPEALIAS
                is KtPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
                is KtTypeReference -> TargetLists.T_TYPE_REFERENCE
                is KtFile -> TargetLists.T_FILE
                is KtTypeParameter -> TargetLists.T_TYPE_PARAMETER
                is KtTypeProjection ->
                    if (annotated.projectionKind == KtProjectionKind.STAR) TargetLists.T_STAR_PROJECTION else TargetLists.T_TYPE_PROJECTION
                is KtAnonymousInitializer -> TargetLists.T_INITIALIZER
                is KtDestructuringDeclaration -> TargetLists.T_DESTRUCTURING_DECLARATION
                is KtLambdaExpression -> TargetLists.T_FUNCTION_LITERAL
                is KtObjectLiteralExpression -> TargetLists.T_OBJECT_LITERAL
                is KtExpression -> TargetLists.T_EXPRESSION
                else -> TargetLists.EMPTY
            }
        }

        private object TargetLists {
            val T_CLASSIFIER = targetList(CLASS)
            val T_TYPEALIAS = targetList(TYPEALIAS)

            val T_LOCAL_VARIABLE = targetList(LOCAL_VARIABLE) {
                onlyWithUseSiteTarget(PROPERTY_SETTER, VALUE_PARAMETER)
            }

            val T_DESTRUCTURING_DECLARATION = targetList(DESTRUCTURING_DECLARATION)

            fun TargetListBuilder.propertyTargets(backingField: Boolean, delegate: Boolean) {
                if (backingField) extraTargets(FIELD)
                if (delegate) {
                    onlyWithUseSiteTarget(VALUE_PARAMETER, PROPERTY_GETTER, PROPERTY_SETTER, FIELD)
                }
                else {
                    onlyWithUseSiteTarget(VALUE_PARAMETER, PROPERTY_GETTER, PROPERTY_SETTER)
                }
            }

            fun T_MEMBER_PROPERTY(backingField: Boolean, delegate: Boolean) =
                    targetList(when {
                        backingField -> MEMBER_PROPERTY_WITH_BACKING_FIELD
                        delegate -> MEMBER_PROPERTY_WITH_DELEGATE
                        else -> MEMBER_PROPERTY_WITHOUT_FIELD_OR_DELEGATE
                    }, MEMBER_PROPERTY, PROPERTY) {
                        propertyTargets(backingField, delegate)
                    }

            fun T_TOP_LEVEL_PROPERTY(backingField: Boolean, delegate: Boolean) =
                    targetList(when {
                        backingField -> TOP_LEVEL_PROPERTY_WITH_BACKING_FIELD
                        delegate -> TOP_LEVEL_PROPERTY_WITH_DELEGATE
                        else -> TOP_LEVEL_PROPERTY_WITHOUT_FIELD_OR_DELEGATE
                    }, TOP_LEVEL_PROPERTY, PROPERTY) {
                        propertyTargets(backingField, delegate)
                    }

            val T_PROPERTY_GETTER = targetList(PROPERTY_GETTER)
            val T_PROPERTY_SETTER = targetList(PROPERTY_SETTER)

            val T_VALUE_PARAMETER_WITHOUT_VAL = targetList(VALUE_PARAMETER)

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

            val T_FUNCTION_LITERAL = targetList(LAMBDA_EXPRESSION, FUNCTION, EXPRESSION)

            val T_FUNCTION_EXPRESSION = targetList(ANONYMOUS_FUNCTION, FUNCTION, EXPRESSION)

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

        private object UseSiteTargetsList {
            val T_CONSTRUCTOR_PARAMETER = listOf(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER,
                                                 AnnotationUseSiteTarget.PROPERTY,
                                                 AnnotationUseSiteTarget.FIELD)

            val T_PROPERTY = listOf(AnnotationUseSiteTarget.PROPERTY,
                                    AnnotationUseSiteTarget.FIELD)
        }
    }
}

interface AdditionalAnnotationChecker {
    fun checkEntries(entries: List<KtAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace)
}