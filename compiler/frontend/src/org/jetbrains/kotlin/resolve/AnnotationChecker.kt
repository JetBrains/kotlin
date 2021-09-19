/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotatedWithKotlinRepeatable
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.isError

class AnnotationChecker(
    private val additionalCheckers: Iterable<AdditionalAnnotationChecker>,
    private val languageVersionSettings: LanguageVersionSettings,
    private val platformAnnotationFeaturesSupport: PlatformAnnotationFeaturesSupport,
) {
    fun check(annotated: KtAnnotated, trace: BindingTrace, descriptor: DeclarationDescriptor? = null) {
        val actualTargets = getActualTargetList(annotated, descriptor, trace.bindingContext)
        checkEntries(annotated.annotationEntries, actualTargets, trace, annotated)
        if (annotated is KtProperty) {
            checkPropertyUseSiteTargetAnnotations(annotated, trace)
        }
        if (annotated is KtClassOrObject) {
            checkSuperTypeAnnotations(annotated, trace)
        }
        if (annotated is KtCallableDeclaration) {
            annotated.typeReference?.let { check(it, trace) }
            annotated.receiverTypeReference?.let { check(it, trace) }
        }
        if (annotated is KtTypeAlias) {
            annotated.getTypeReference()?.let { check(it, trace) }
        }
        if (
            annotated is KtTypeParameterListOwner &&
            (annotated is KtCallableDeclaration || languageVersionSettings.supportsFeature(ProperCheckAnnotationsTargetInTypeUsePositions) ||
                    (annotated is KtClass && languageVersionSettings.supportsFeature(ClassTypeParameterAnnotations)))
        ) {
            if (annotated is KtClass && languageVersionSettings.supportsFeature(ClassTypeParameterAnnotations)) {
                (descriptor as? ClassDescriptor)?.declaredTypeParameters?.forEach {
                    //force annotation resolve to obtain targets
                    ForceResolveUtil.forceResolveAllContents(it.annotations)
                }
            }

            annotated.typeParameters.forEach { check(it, trace) }
            for (typeParameter in annotated.typeParameters) {
                typeParameter.extendsBound?.let {
                    checkTypeReference(
                        it,
                        trace,
                        shouldCheckReferenceItself = true,
                        checkWithoutLanguageFeature = annotated is KtCallableDeclaration
                    )
                }
            }
            for (typeConstraint in annotated.typeConstraints) {
                typeConstraint.boundTypeReference?.let { checkTypeReference(it, trace, shouldCheckReferenceItself = true) }
            }
        }
        if (annotated is KtTypeReference) {
            if (languageVersionSettings.supportsFeature(ProperCheckAnnotationsTargetInTypeUsePositions)) {
                checkTypeReference(annotated, trace)
            } else {
                annotated.typeElement?.typeArgumentsAsTypes?.filterNotNull()?.forEach { check(it, trace) }
            }
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
        checkEntries(
            expression.getAnnotationEntries(),
            getActualTargetList(expression, null, trace.bindingContext),
            trace,
            expression.parent as? KtAnnotated
        )
        if (expression is KtCallElement && languageVersionSettings.supportsFeature(ProperCheckAnnotationsTargetInTypeUsePositions)) {
            val typeArguments = expression.typeArguments.mapNotNull { it.typeReference }
            for (typeArgument in typeArguments) {
                checkEntries(typeArgument.annotationEntries, getActualTargetList(typeArgument, null, trace.bindingContext), trace)
            }
        }
        if (expression is KtLambdaExpression) {
            for (parameter in expression.valueParameters) {
                parameter.typeReference?.let { check(it, trace) }
            }
        }
    }

    private fun checkPropertyUseSiteTargetAnnotations(property: KtProperty, trace: BindingTrace) {
        fun List<KtAnnotationEntry>?.getDescriptors() =
            this?.mapNotNull { trace.get(BindingContext.ANNOTATION, it)?.annotationClass } ?: listOf()

        val reportError = languageVersionSettings.supportsFeature(ProhibitRepeatedUseSiteTargetAnnotations)

        val propertyAnnotations = mapOf(
            AnnotationUseSiteTarget.PROPERTY_GETTER to property.getter?.annotationEntries.getDescriptors(),
            AnnotationUseSiteTarget.PROPERTY_SETTER to property.setter?.annotationEntries.getDescriptors(),
            AnnotationUseSiteTarget.SETTER_PARAMETER to property.setter?.parameter?.annotationEntries.getDescriptors()
        )

        for (entry in property.annotationEntries) {
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            val classDescriptor = descriptor.annotationClass ?: continue

            val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget() ?: property.getDefaultUseSiteTarget(descriptor)
            val existingAnnotations = propertyAnnotations[useSiteTarget] ?: continue
            if (classDescriptor in existingAnnotations && !isRepeatableAnnotation(classDescriptor)) {
                if (reportError) {
                    trace.reportDiagnosticOnce(Errors.REPEATED_ANNOTATION.on(entry))
                } else {
                    trace.report(Errors.REPEATED_ANNOTATION_WARNING.on(entry))
                }
            }
        }
    }

    private fun checkSuperTypeAnnotations(annotated: KtClassOrObject, trace: BindingTrace) {
        for (superType in annotated.superTypeListEntries.mapNotNull { it.typeReference }) {
            checkTypeReference(superType, trace, isSuperType = true)
        }
    }

    private fun checkTypeReference(
        reference: KtTypeReference,
        trace: BindingTrace,
        isSuperType: Boolean = false,
        shouldCheckReferenceItself: Boolean = false,
        checkWithoutLanguageFeature: Boolean = false
    ) {
        val shouldRunCheck = isSuperType || shouldCheckReferenceItself
        if (shouldRunCheck) {
            for (entry in reference.annotationEntries) {
                val descriptor = trace.get(BindingContext.ANNOTATION, entry)
                if (descriptor is LazyAnnotationDescriptor) {
                    /*
                     * There are no users of type annotations until backend, so if there are errors
                     *  in annotation call then we should force resolve of it to detect and
                     *  report them
                     */
                    descriptor.forceResolveAllContents()
                }
                val actualTargets = getActualTargetList(reference, null, trace.bindingContext)
                if (entry.useSiteTarget != null && isSuperType) {
                    trace.report(Errors.ANNOTATION_ON_SUPERCLASS.on(languageVersionSettings, entry))
                } else if (shouldRunCheck && (languageVersionSettings.supportsFeature(ProperCheckAnnotationsTargetInTypeUsePositions) || checkWithoutLanguageFeature)) {
                    checkAnnotationEntry(entry, actualTargets, trace)
                }
            }
        }
        val typeArguments = reference.typeElement?.typeArgumentsAsTypes ?: return
        for (typeArgument in typeArguments) {
            typeArgument?.let { checkTypeReference(it, trace, shouldCheckReferenceItself = true) }
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
            KotlinTarget.USE_SITE_MAPPING[it] in applicableTargetSet(descriptor)
        }

    private fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: TargetList,
        trace: BindingTrace,
        annotated: KtAnnotated? = null
    ) {
        if (entries.isEmpty()) return

        val entryTypesWithAnnotations = hashMapOf<KotlinType, MutableList<AnnotationUseSiteTarget?>>()

        for (entry in entries) {
            checkAnnotationEntry(entry, actualTargets, trace)
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            if (descriptor is LazyAnnotationDescriptor) {
                /*
                 * There are no users of type annotations until backend, so if there are errors
                 *  in annotation call then we should force resolve of it to detect and
                 *  report them
                 */
                descriptor.forceResolveAllContents()
            }

            val contextReceiversSupported = languageVersionSettings.supportsFeature(ContextReceivers)
            if (descriptor.fqName == FqName("kotlin.ContextFunctionTypeParams") && !contextReceiversSupported) {
                trace.report(Errors.UNSUPPORTED_FEATURE.on(entry, ContextReceivers to languageVersionSettings))
            }

            val classDescriptor = descriptor.annotationClass ?: continue

            val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget() ?: annotated.getDefaultUseSiteTarget(descriptor)
            val existingTargetsForAnnotation = entryTypesWithAnnotations.getOrPut(descriptor.type) { arrayListOf() }
            val duplicateAnnotation = useSiteTarget in existingTargetsForAnnotation
                    || (existingTargetsForAnnotation.any { (it == null) != (useSiteTarget == null) })

            if (duplicateAnnotation && !isRepeatableAnnotation(classDescriptor)) {
                trace.report(Errors.REPEATED_ANNOTATION.on(entry))
            }

            existingTargetsForAnnotation.add(useSiteTarget)
        }

        for (checker in additionalCheckers) {
            checker.checkEntries(entries, actualTargets.defaultTargets, trace, annotated, languageVersionSettings)
        }
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

        fun checkWithUseSiteTargets(): Boolean {
            if (useSiteTarget == null) return false

            val useSiteMapping = KotlinTarget.USE_SITE_MAPPING[useSiteTarget]
            return actualTargets.onlyWithUseSiteTarget.any { it in applicableTargets && it == useSiteMapping }
        }

        if (check(actualTargets.defaultTargets) || check(actualTargets.canBeSubstituted) || checkWithUseSiteTargets()) {
            checkUselessFunctionLiteralAnnotation()
            return
        }

        if (useSiteTarget != null) {
            trace.report(
                Errors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET.on(
                    entry, actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target", useSiteTarget.renderName
                )
            )
        } else {
            trace.report(
                Errors.WRONG_ANNOTATION_TARGET.on(
                    entry, actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target"
                )
            )
        }
    }

    private fun isRepeatableAnnotation(descriptor: ClassDescriptor): Boolean =
        descriptor.isAnnotatedWithKotlinRepeatable() || platformAnnotationFeaturesSupport.isRepeatableAnnotationClass(descriptor)

    companion object {
        private val TARGET_ALLOWED_TARGETS = Name.identifier("allowedTargets")

        private fun applicableTargetSet(entry: KtAnnotationEntry, trace: BindingTrace): Set<KotlinTarget> {
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: return KotlinTarget.DEFAULT_TARGET_SET
            // For descriptor with error type, all targets are considered as possible
            if (descriptor.type.isError) return KotlinTarget.ALL_TARGET_SET
            return descriptor.annotationClass?.let(this::applicableTargetSetFromTargetAnnotationOrNull) ?: KotlinTarget.DEFAULT_TARGET_SET
        }

        @JvmStatic
        fun applicableTargetSet(descriptor: AnnotationDescriptor): Set<KotlinTarget> {
            val classDescriptor = descriptor.annotationClass ?: return emptySet()
            return applicableTargetSet(classDescriptor)
        }

        fun applicableTargetSet(classDescriptor: ClassDescriptor): Set<KotlinTarget> {
            val targetEntryDescriptor = classDescriptor.annotations.findAnnotation(StandardNames.FqNames.target)
            return targetEntryDescriptor?.let { loadAnnotationTargets(it) } ?: KotlinTarget.DEFAULT_TARGET_SET
        }

        fun applicableTargetSetFromTargetAnnotationOrNull(classDescriptor: ClassDescriptor): Set<KotlinTarget>? {
            val targetEntryDescriptor = classDescriptor.annotations.findAnnotation(StandardNames.FqNames.target) ?: return null
            return loadAnnotationTargets(targetEntryDescriptor)
        }

        fun loadAnnotationTargets(targetEntryDescriptor: AnnotationDescriptor): Set<KotlinTarget>? {
            val valueArgument = targetEntryDescriptor.allValueArguments[TARGET_ALLOWED_TARGETS] as? ArrayValue ?: return null
            return valueArgument.value.filterIsInstance<EnumValue>().mapNotNull {
                KotlinTarget.valueOrNull(it.enumEntryName.asString())
            }.toSet()
        }

        fun getDeclarationSiteActualTargetList(annotated: KtElement, descriptor: ClassDescriptor?, context: BindingContext):
                List<KotlinTarget> {
            return getActualTargetList(annotated, descriptor, context).defaultTargets
        }

        private fun DeclarationDescriptor?.hasBackingField(context: BindingContext) =
            (this as? PropertyDescriptor)?.let { context.get(BindingContext.BACKING_FIELD_REQUIRED, it) } ?: false

        fun getActualTargetList(annotated: KtElement, descriptor: DeclarationDescriptor?, context: BindingContext): TargetList {
            return when (annotated) {
                is KtClassOrObject -> {
                    (descriptor as? ClassDescriptor)?.let {
                        TargetList(
                            KotlinTarget.classActualTargets(
                                it.kind,
                                isInnerClass = it.isInner,
                                isCompanionObject = it.isCompanionObject,
                                isLocalClass = DescriptorUtils.isLocal(it)
                            )
                        )
                    } ?: TargetLists.T_CLASSIFIER
                }
                is KtDestructuringDeclarationEntry -> TargetLists.T_LOCAL_VARIABLE
                is KtProperty -> {
                    when {
                        annotated.isLocal -> TargetLists.T_LOCAL_VARIABLE
                        annotated.isMember -> TargetLists.T_MEMBER_PROPERTY(descriptor.hasBackingField(context), annotated.hasDelegate())
                        else -> TargetLists.T_TOP_LEVEL_PROPERTY(descriptor.hasBackingField(context), annotated.hasDelegate())
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
    }
}

private typealias TargetLists = AnnotationTargetLists

private typealias TargetList = AnnotationTargetList

interface AdditionalAnnotationChecker {
    fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
        languageVersionSettings: LanguageVersionSettings
    )
}
