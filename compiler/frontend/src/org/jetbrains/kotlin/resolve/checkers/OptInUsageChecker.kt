/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OLD_EXPERIMENTAL_FQ_NAME
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OLD_USE_EXPERIMENTAL_FQ_NAME
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_FQ_NAME
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_FQ_NAMES
import org.jetbrains.kotlin.resolve.checkers.OptInNames.REQUIRES_OPT_IN_FQ_NAME
import org.jetbrains.kotlin.resolve.checkers.OptInNames.REQUIRES_OPT_IN_FQ_NAMES
import org.jetbrains.kotlin.resolve.checkers.OptInNames.USE_EXPERIMENTAL_ANNOTATION_CLASS
import org.jetbrains.kotlin.resolve.checkers.OptInNames.WAS_EXPERIMENTAL_FQ_NAME
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationSettings
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull

class OptInUsageChecker(project: Project) : CallChecker {
    private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

    interface OptInDiagnosticReporter {
        fun report(trace: BindingTrace, element: PsiElement, fqName: FqName, message: String?)
    }

    class OptInFactoryBasedReporter(
        val factory: DiagnosticFactory2<PsiElement, FqName, String>,
        val defaultMessage: (FqName) -> String
    ) : OptInDiagnosticReporter {
        override fun report(trace: BindingTrace, element: PsiElement, fqName: FqName, message: String?) {
            trace.reportDiagnosticOnce(factory.on(element, fqName, message?.takeIf { it.isNotBlank() } ?: defaultMessage(fqName)))
        }
    }

    data class OptInReporterMultiplexer(
        val warning: OptInDiagnosticReporter,
        val error: OptInDiagnosticReporter,
        val futureError: OptInDiagnosticReporter
    )

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        val optIns = resultingDescriptor.loadOptIns(moduleAnnotationsResolver, context.languageVersionSettings)
        if (resultingDescriptor is FunctionDescriptor &&
            resultingDescriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
        ) {
            val propertyDescriptor = resultingDescriptor.findRelevantDataClassPropertyIfAny(context)
            if (propertyDescriptor != null) {
                reportNotAllowedOptIns(
                    optIns + propertyDescriptor.loadOptIns(
                        moduleAnnotationsResolver, context.languageVersionSettings
                    ), reportOn, context
                )
                return
            }
        }
        if (resultingDescriptor is SamConstructorDescriptor) {
            // KT-47708 special case (warning only)
            val methodDescriptor = resultingDescriptor.getSingleAbstractMethod()
            val samOptIns = methodDescriptor.loadOptIns(moduleAnnotationsResolver, context.languageVersionSettings)
                .map { (fqName, _, message) ->
                    OptInDescription(fqName, OptInDescription.Severity.WARNING, message)
                }
            reportNotAllowedOptIns(samOptIns, reportOn, context)
        }
        reportNotAllowedOptIns(optIns, reportOn, context)
    }

    private fun FunctionDescriptor.findRelevantDataClassPropertyIfAny(context: CallCheckerContext): PropertyDescriptor? {
        val index = name.asString().removePrefix("component").toIntOrNull()
        val container = containingDeclaration
        if (container is ClassDescriptor && container.isData && index != null) {
            val dataClassParameterDescriptor = container.unsubstitutedPrimaryConstructor?.valueParameters?.getOrNull(index - 1)
            if (dataClassParameterDescriptor != null) {
                return context.trace.bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, dataClassParameterDescriptor]
            }
        }
        return null
    }

    companion object {
        private val LEVEL = Name.identifier("level")
        private val MESSAGE = Name.identifier("message")
        private val WARNING_LEVEL = Name.identifier("WARNING")
        private val ERROR_LEVEL = Name.identifier("ERROR")

        internal fun getDefaultDiagnosticMessage(prefix: String): (FqName) -> String = { fqName: FqName ->
            OptInNames.buildDefaultDiagnosticMessage(prefix, fqName.asString())
        }

        private val USAGE_DIAGNOSTICS = OptInReporterMultiplexer(
            warning = OptInFactoryBasedReporter(
                Errors.OPT_IN_USAGE,
                getDefaultDiagnosticMessage(OptInNames.buildMessagePrefix("should"))
            ),
            error = OptInFactoryBasedReporter(
                Errors.OPT_IN_USAGE_ERROR,
                getDefaultDiagnosticMessage(OptInNames.buildMessagePrefix("must"))
            ),
            futureError = OptInFactoryBasedReporter(
                Errors.OPT_IN_USAGE_FUTURE_ERROR,
                getDefaultDiagnosticMessage("This declaration is experimental due to signature types and its usage must be marked (will become an error in 1.6)")
            ),
        )

        fun reportNotAllowedOptIns(
            descriptions: Collection<OptInDescription>, element: PsiElement, context: CheckerContext
        ) {
            reportNotAllowedOptIns(
                descriptions, element, context.languageVersionSettings, context.trace, USAGE_DIAGNOSTICS
            )
        }

        fun reportNotAllowedOptIns(
            descriptions: Collection<OptInDescription>,
            element: PsiElement,
            languageVersionSettings: LanguageVersionSettings,
            trace: BindingTrace,
            diagnostics: OptInReporterMultiplexer
        ) {
            for ((annotationFqName, severity, message) in descriptions) {
                if (!element.isOptInAllowed(annotationFqName, languageVersionSettings, trace.bindingContext)) {
                    val diagnostic = when (severity) {
                        OptInDescription.Severity.WARNING -> diagnostics.warning
                        OptInDescription.Severity.ERROR -> diagnostics.error
                        OptInDescription.Severity.FUTURE_ERROR -> diagnostics.futureError
                    }
                    diagnostic.report(trace, element, annotationFqName, message)
                }
            }
        }

        fun DeclarationDescriptor.loadOptIns(
            moduleAnnotationsResolver: ModuleAnnotationsResolver,
            languageVersionSettings: LanguageVersionSettings,
            visited: MutableSet<DeclarationDescriptor> = mutableSetOf(),
            useFutureError: Boolean = false,
            useMarkersFromContainer: Boolean = true,
        ): Set<OptInDescription> {
            if (!visited.add(this)) return emptySet()
            val result = SmartSet.create<OptInDescription>()
            if (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                for (overridden in overriddenDescriptors) {
                    result.addAll(
                        overridden.loadOptIns(
                            moduleAnnotationsResolver,
                            languageVersionSettings,
                            visited,
                            useFutureError = !languageVersionSettings.supportsFeature(LanguageFeature.OptInContagiousSignatures),
                            useMarkersFromContainer = false
                        )
                    )
                    if (useMarkersFromContainer) {
                        (containingDeclaration as? ClassDescriptor)?.let {
                            result.addAll(
                                it.loadOptIns(moduleAnnotationsResolver, languageVersionSettings, visited, useFutureError)
                            )
                        }
                    }
                }
                return result
            }

            for (annotation in annotations) {
                result.addIfNotNull(annotation.annotationClass?.loadOptInForMarkerAnnotation(useFutureError))
            }

            if (this is CallableDescriptor && this !is ClassConstructorDescriptor) {
                result.addAll(
                    returnType.loadOptIns(moduleAnnotationsResolver, languageVersionSettings, visited)
                )
                result.addAll(
                    extensionReceiverParameter?.type.loadOptIns(
                        moduleAnnotationsResolver, languageVersionSettings, visited
                    )
                )
                if (this is FunctionDescriptor) {
                    valueParameters.forEach {
                        result.addAll(
                            it.type.loadOptIns(
                                moduleAnnotationsResolver, languageVersionSettings, visited
                            )
                        )
                    }
                }
            }

            if (this is TypeAliasDescriptor) {
                result.addAll(expandedType.loadOptIns(moduleAnnotationsResolver, languageVersionSettings, visited))
            }

            if (annotations.any { it.fqName == WAS_EXPERIMENTAL_FQ_NAME }) {
                val accessibility = checkSinceKotlinVersionAccessibility(languageVersionSettings)
                if (accessibility is SinceKotlinAccessibility.NotAccessibleButWasExperimental) {
                    result.addAll(accessibility.markerClasses.mapNotNull { it.loadOptInForMarkerAnnotation() })
                }
            }

            val container = containingDeclaration
            if (useMarkersFromContainer && container is ClassDescriptor && this !is ConstructorDescriptor) {
                result.addAll(container.loadOptIns(moduleAnnotationsResolver, languageVersionSettings, visited, useFutureError))
            }

            return result
        }

        private fun KotlinType?.loadOptIns(
            moduleAnnotationsResolver: ModuleAnnotationsResolver,
            languageVersionSettings: LanguageVersionSettings,
            visitedClassifiers: MutableSet<DeclarationDescriptor>
        ): Set<OptInDescription> =
            when {
                this?.isError != false -> emptySet()
                this is AbbreviatedType -> abbreviation.constructor.declarationDescriptor?.loadOptIns(
                    moduleAnnotationsResolver, languageVersionSettings, visitedClassifiers,
                    useFutureError = !languageVersionSettings.supportsFeature(LanguageFeature.OptInContagiousSignatures)
                ).orEmpty() + expandedType.loadOptIns(
                    moduleAnnotationsResolver, languageVersionSettings, visitedClassifiers
                )
                else -> constructor.declarationDescriptor?.loadOptIns(
                    moduleAnnotationsResolver, languageVersionSettings, visitedClassifiers,
                    useFutureError = !languageVersionSettings.supportsFeature(LanguageFeature.OptInContagiousSignatures)
                ).orEmpty() + arguments.flatMap {
                    if (it.isStarProjection) emptySet()
                    else it.type.loadOptIns(moduleAnnotationsResolver, languageVersionSettings, visitedClassifiers)
                }
            }

        internal fun ClassDescriptor.loadOptInForMarkerAnnotation(useFutureError: Boolean = false): OptInDescription? {
            val optInAnnotationDescriptor =
                annotations.findAnnotation(REQUIRES_OPT_IN_FQ_NAME)
                    ?: annotations.findAnnotation(OLD_EXPERIMENTAL_FQ_NAME)
                    ?: return null

            val arguments = optInAnnotationDescriptor.allValueArguments
            val severity = when ((arguments[LEVEL] as? EnumValue)?.enumEntryName) {
                WARNING_LEVEL -> OptInDescription.Severity.WARNING
                ERROR_LEVEL -> if (useFutureError) OptInDescription.Severity.FUTURE_ERROR else OptInDescription.Severity.ERROR
                else -> if (OptInDescription.DEFAULT_SEVERITY == OptInDescription.Severity.ERROR && useFutureError) {
                    OptInDescription.Severity.FUTURE_ERROR
                } else {
                    OptInDescription.DEFAULT_SEVERITY
                }
            }

            val message = (arguments[MESSAGE] as? StringValue)?.value

            return OptInDescription(fqNameSafe, severity, message)
        }

        private fun PsiElement.isOptInAllowed(annotationFqName: FqName, context: CheckerContext): Boolean =
            isOptInAllowed(annotationFqName, context.languageVersionSettings, context.trace.bindingContext)

        /**
         * Checks whether there's an element lexically above in the tree, annotated with `@UseExperimental(X::class)`, or a declaration
         * annotated with `@X` where [annotationFqName] is the FQ name of X
         */
        fun PsiElement.isOptInAllowed(
            annotationFqName: FqName,
            languageVersionSettings: LanguageVersionSettings,
            bindingContext: BindingContext
        ): Boolean =
            annotationFqName.asString() in languageVersionSettings.getFlag(AnalysisFlags.optIn) ||
                    anyParentMatches { element ->
                        element.isDeclarationAnnotatedWith(annotationFqName, bindingContext) ||
                                element.isElementAnnotatedWithOptIn(annotationFqName, bindingContext)
                    }

        private fun PsiElement.isDeclarationAnnotatedWith(annotationFqName: FqName, bindingContext: BindingContext): Boolean {
            if (this !is KtDeclaration) return false

            val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
            return descriptor != null && descriptor.annotations.hasAnnotation(annotationFqName)
        }

        private fun PsiElement.isElementAnnotatedWithOptIn(annotationFqName: FqName, bindingContext: BindingContext): Boolean {
            return this is KtAnnotated && annotationEntries.any { entry ->
                val descriptor = bindingContext.get(BindingContext.ANNOTATION, entry)
                if (descriptor != null && descriptor.fqName in OPT_IN_FQ_NAMES) {
                    val annotationClasses = descriptor.allValueArguments[USE_EXPERIMENTAL_ANNOTATION_CLASS]
                    annotationClasses is ArrayValue && annotationClasses.value.any { annotationClass ->
                        annotationClass is KClassValue && annotationClass.value.let { value ->
                            value is KClassValue.Value.NormalClass &&
                                    value.classId.asSingleFqName() == annotationFqName && value.arrayDimensions == 0
                        }
                    }
                } else false
            }
        }

        private inline fun PsiElement.anyParentMatches(predicate: (element: PsiElement) -> Boolean): Boolean {
            var element = this
            while (true) {
                if (predicate(element)) return true
                element = element.parent ?: return false
            }
        }

        fun checkCompilerArguments(
            module: ModuleDescriptor,
            languageVersionSettings: LanguageVersionSettings,
            reportError: (String) -> Unit,
            reportWarning: (String) -> Unit
        ) {
            // Ideally, we should run full resolution (with all classifier usage checkers) on classifiers used in
            // "-opt-in" arguments. However, it's not easy to do this. This should be solved in the future with the support of
            // module annotations. For now, we only check deprecations because this is needed to correctly retire unneeded compiler arguments.
            val deprecationResolver = DeprecationResolver(
                LockBasedStorageManager("OptInUsageChecker"),
                languageVersionSettings,
                DeprecationSettings.Default
            )

            // Returns true if fqName resolves to a valid opt-in requirement marker.
            fun checkAnnotation(fqName: String): Boolean {
                val descriptor = module.resolveClassByFqName(FqName(fqName), NoLookupLocation.FOR_NON_TRACKED_SCOPE)
                if (descriptor == null) {
                    reportWarning("Opt-in requirement marker $fqName is unresolved. Please make sure it's present in the module dependencies")
                    return false
                }

                if (descriptor.loadOptInForMarkerAnnotation() == null) {
                    reportWarning("Class $fqName is not an opt-in requirement marker")
                    return false
                }

                for (deprecation in deprecationResolver.getDeprecations(descriptor)) {
                    val report = when (deprecation.deprecationLevel) {
                        DeprecationLevelValue.WARNING -> reportWarning
                        DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> reportError
                    }
                    report("Opt-in requirement marker $fqName is deprecated" + deprecation.message?.let { ". $it" }.orEmpty())
                }
                return true
            }

            languageVersionSettings.getFlag(AnalysisFlags.optIn).forEach { fqName ->
                if (fqName != REQUIRES_OPT_IN_FQ_NAME.asString() && fqName != OLD_EXPERIMENTAL_FQ_NAME.asString()) {
                    checkAnnotation(fqName)
                }
            }
        }
    }

    class ClassifierUsage(project: Project) : ClassifierUsageChecker {
        private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

        override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
            val name = targetDescriptor.name
            if (name == OLD_EXPERIMENTAL_FQ_NAME.shortName() || name == REQUIRES_OPT_IN_FQ_NAME.shortName() ||
                name == OLD_USE_EXPERIMENTAL_FQ_NAME.shortName() || name == OPT_IN_FQ_NAME.shortName()
            ) {
                val fqName = targetDescriptor.fqNameSafe
                if (fqName in REQUIRES_OPT_IN_FQ_NAMES || fqName in OPT_IN_FQ_NAMES) {
                    checkUsageOfKotlinExperimentalOrOptIn(element, context)
                    return
                }
            }

            val targetClass = when (targetDescriptor) {
                is ClassDescriptor -> targetDescriptor
                is TypeAliasDescriptor -> targetDescriptor.classDescriptor
                else -> null
            }
            if (targetClass != null && targetClass.loadOptInForMarkerAnnotation() != null) {
                if (!element.isUsageAsAnnotationOrImport() &&
                    !element.isUsageAsOptInArgument(context.trace.bindingContext)
                ) {
                    context.trace.report(
                        Errors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN.on(element)
                    )
                }
            }

            if (element.getParentOfType<KtImportDirective>(false) == null) {
                val descriptions = mutableSetOf<OptInDescription>()
                descriptions += targetDescriptor.loadOptIns(moduleAnnotationsResolver, context.languageVersionSettings)
                if (targetDescriptor is TypeAliasDescriptor) {
                    descriptions.addAll(
                        targetDescriptor.expandedType.loadOptIns(
                            moduleAnnotationsResolver, context.languageVersionSettings, mutableSetOf(targetDescriptor)
                        )
                    )
                }
                reportNotAllowedOptIns(descriptions, element, context)
            }
        }

        private fun checkUsageOfKotlinExperimentalOrOptIn(element: PsiElement, context: CheckerContext) {
            val optInFqNames = context.languageVersionSettings.getFlag(AnalysisFlags.optIn)
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.OptInRelease) &&
                REQUIRES_OPT_IN_FQ_NAME.asString() !in optInFqNames &&
                OLD_EXPERIMENTAL_FQ_NAME.asString() !in optInFqNames
            ) {
                context.trace.report(Errors.OPT_IN_IS_NOT_ENABLED.on(element))
            }

            if (!element.isUsageAsAnnotationOrImport() && !element.isUsageAsQualifier()) {
                context.trace.report(Errors.OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION.on(element))
            }
        }

        private fun PsiElement.isUsageAsQualifier(): Boolean {
            if (this is KtSimpleNameExpression) {
                val qualifier = getTopmostParentQualifiedExpressionForSelector() ?: this
                if ((qualifier.parent as? KtDotQualifiedExpression)?.receiverExpression == qualifier) {
                    return true
                }
            }

            return false
        }

        private fun PsiElement.isUsageAsOptInArgument(bindingContext: BindingContext): Boolean {
            val qualifier = (this as? KtSimpleNameExpression)?.getTopmostParentQualifiedExpressionForSelector() ?: this
            val parent = qualifier.parent

            return parent is KtClassLiteralExpression &&
                    parent.parent is KtValueArgument &&
                    parent.parent.parent is KtValueArgumentList &&
                    parent.parent.parent.parent.let { entry ->
                        entry is KtAnnotationEntry && bindingContext.get(BindingContext.ANNOTATION, entry)?.let { annotation ->
                            annotation.fqName in OPT_IN_FQ_NAMES || annotation.fqName == WAS_EXPERIMENTAL_FQ_NAME
                        } == true
                    }
        }
    }

    class Overrides(project: Project) : DeclarationChecker {
        private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

        override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
            if (descriptor !is CallableMemberDescriptor) return

            val optInOverriddenDescriptorMap = descriptor.overriddenDescriptors.flatMap { overriddenMember ->
                overriddenMember.loadOptIns(moduleAnnotationsResolver, context.languageVersionSettings)
                    .map { description -> description to overriddenMember }
            }.toMap()

            for ((description, overriddenMember) in optInOverriddenDescriptorMap) {
                if (!declaration.isOptInAllowed(description.annotationFqName, context)) {
                    val reportOn = (declaration as? KtNamedDeclaration)?.nameIdentifier ?: declaration

                    val (diagnostic, defaultMessageVerb) = when (description.severity) {
                        OptInDescription.Severity.WARNING -> Errors.OPT_IN_OVERRIDE to "should"
                        OptInDescription.Severity.ERROR -> Errors.OPT_IN_OVERRIDE_ERROR to "must"
                        OptInDescription.Severity.FUTURE_ERROR -> Errors.OPT_IN_OVERRIDE_ERROR to "must"
                    }
                    val message = OptInNames.buildOverrideMessage(
                        supertypeName = overriddenMember.containingDeclaration.name.asString(),
                        markerMessage = description.message,
                        verb = defaultMessageVerb,
                        markerName = description.annotationFqName.asString()
                    )
                    context.trace.report(diagnostic.on(reportOn, description.annotationFqName, message))
                }
            }
        }
    }
}
