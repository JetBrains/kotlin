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
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
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
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.deprecation.CoroutineCompatibilitySupport
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationSettings
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull

class ExperimentalUsageChecker(project: Project) : CallChecker {
    private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

    data class Experimentality(val annotationFqName: FqName, val severity: Severity) {
        enum class Severity { WARNING, ERROR }

        companion object {
            val DEFAULT_SEVERITY = Severity.ERROR
        }
    }

    data class ExperimentalityDiagnostics(
        val warning: DiagnosticFactory1<PsiElement, FqName>,
        val error: DiagnosticFactory1<PsiElement, FqName>
    )

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val experimentalities =
            resolvedCall.resultingDescriptor.loadExperimentalities(moduleAnnotationsResolver, context.languageVersionSettings)
        reportNotAcceptedExperimentalities(experimentalities, reportOn, context)
    }

    companion object {
        val OLD_EXPERIMENTAL_FQ_NAME = FqName("kotlin.Experimental")
        val OLD_USE_EXPERIMENTAL_FQ_NAME = FqName("kotlin.UseExperimental")
        val REQUIRES_OPT_IN_FQ_NAME = FqName("kotlin.RequiresOptIn")
        val OPT_IN_FQ_NAME = FqName("kotlin.OptIn")

        val EXPERIMENTAL_FQ_NAMES = setOf(OLD_EXPERIMENTAL_FQ_NAME, REQUIRES_OPT_IN_FQ_NAME)
        val USE_EXPERIMENTAL_FQ_NAMES = setOf(OLD_USE_EXPERIMENTAL_FQ_NAME, OPT_IN_FQ_NAME)

        internal val WAS_EXPERIMENTAL_FQ_NAME = FqName("kotlin.WasExperimental")
        internal val USE_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")
        internal val WAS_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")

        private val LEVEL = Name.identifier("level")
        private val WARNING_LEVEL = Name.identifier("WARNING")
        private val ERROR_LEVEL = Name.identifier("ERROR")

        private val EXPERIMENTAL_API_DIAGNOSTICS = ExperimentalityDiagnostics(
            Errors.EXPERIMENTAL_API_USAGE, Errors.EXPERIMENTAL_API_USAGE_ERROR
        )

        fun reportNotAcceptedExperimentalities(
            experimentalities: Collection<Experimentality>, element: PsiElement, context: CheckerContext
        ) {
            reportNotAcceptedExperimentalities(
                experimentalities, element, context.languageVersionSettings, context.trace, EXPERIMENTAL_API_DIAGNOSTICS
            )
        }

        fun reportNotAcceptedExperimentalities(
            experimentalities: Collection<Experimentality>,
            element: PsiElement,
            languageVersionSettings: LanguageVersionSettings,
            trace: BindingTrace,
            diagnostics: ExperimentalityDiagnostics
        ) {
            for ((annotationFqName, severity) in experimentalities) {
                if (!element.isExperimentalityAccepted(annotationFqName, languageVersionSettings, trace.bindingContext)) {
                    val diagnostic = when (severity) {
                        Experimentality.Severity.WARNING -> diagnostics.warning
                        Experimentality.Severity.ERROR -> diagnostics.error
                    }
                    trace.reportDiagnosticOnce(diagnostic.on(element, annotationFqName))
                }
            }
        }

        fun DeclarationDescriptor.loadExperimentalities(
            moduleAnnotationsResolver: ModuleAnnotationsResolver,
            languageVersionSettings: LanguageVersionSettings
        ): Set<Experimentality> {
            val result = SmartSet.create<Experimentality>()

            for (annotation in annotations) {
                result.addIfNotNull(annotation.annotationClass?.loadExperimentalityForMarkerAnnotation())
            }

            if (annotations.any { it.fqName == WAS_EXPERIMENTAL_FQ_NAME }) {
                val accessibility = checkSinceKotlinVersionAccessibility(languageVersionSettings)
                if (accessibility is SinceKotlinAccessibility.NotAccessibleButWasExperimental) {
                    result.addAll(accessibility.markerClasses.map { it.loadExperimentalityForMarkerAnnotation() })
                }
            }

            val container = containingDeclaration
            if (container is ClassDescriptor && this !is ConstructorDescriptor) {
                result.addAll(container.loadExperimentalities(moduleAnnotationsResolver, languageVersionSettings))
            }

            for (moduleAnnotationClassId in moduleAnnotationsResolver.getAnnotationsOnContainingModule(this)) {
                val annotationClass = module.findClassAcrossModuleDependencies(moduleAnnotationClassId)
                result.addIfNotNull(annotationClass?.loadExperimentalityForMarkerAnnotation())
            }

            return result
        }

        internal fun ClassDescriptor.loadExperimentalityForMarkerAnnotation(): Experimentality? {
            val experimental =
                annotations.findAnnotation(REQUIRES_OPT_IN_FQ_NAME)
                    ?: annotations.findAnnotation(OLD_EXPERIMENTAL_FQ_NAME)
                    ?: return null

            val severity = when ((experimental.allValueArguments[LEVEL] as? EnumValue)?.enumEntryName) {
                WARNING_LEVEL -> Experimentality.Severity.WARNING
                ERROR_LEVEL -> Experimentality.Severity.ERROR
                else -> Experimentality.DEFAULT_SEVERITY
            }

            return Experimentality(fqNameSafe, severity)
        }

        private fun PsiElement.isExperimentalityAccepted(annotationFqName: FqName, context: CheckerContext): Boolean =
            isExperimentalityAccepted(annotationFqName, context.languageVersionSettings, context.trace.bindingContext)

        /**
         * Checks whether there's an element lexically above in the tree, annotated with `@UseExperimental(X::class)`, or a declaration
         * annotated with `@X` where [annotationFqName] is the FQ name of X
         */
        fun PsiElement.isExperimentalityAccepted(
            annotationFqName: FqName,
            languageVersionSettings: LanguageVersionSettings,
            bindingContext: BindingContext
        ): Boolean =
            annotationFqName.asString() in languageVersionSettings.getFlag(AnalysisFlags.experimental) ||
                    annotationFqName.asString() in languageVersionSettings.getFlag(AnalysisFlags.useExperimental) ||
                    anyParentMatches { element ->
                        element.isDeclarationAnnotatedWith(annotationFqName, bindingContext) ||
                                element.isElementAnnotatedWithUseExperimentalOf(annotationFqName, bindingContext)
                    }

        private fun PsiElement.isDeclarationAnnotatedWith(annotationFqName: FqName, bindingContext: BindingContext): Boolean {
            if (this !is KtDeclaration) return false

            val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
            return descriptor != null && !DescriptorUtils.isLocal(descriptor) &&
                    descriptor.annotations.hasAnnotation(annotationFqName)
        }

        private fun PsiElement.isElementAnnotatedWithUseExperimentalOf(annotationFqName: FqName, bindingContext: BindingContext): Boolean {
            return this is KtAnnotated && annotationEntries.any { entry ->
                val descriptor = bindingContext.get(BindingContext.ANNOTATION, entry)
                if (descriptor != null && descriptor.fqName in USE_EXPERIMENTAL_FQ_NAMES) {
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
            // Ideally, we should run full resolution (with all classifier usage checkers) on classifiers used in "-Xexperimental" and
            // "-Xuse-experimental" arguments. However, it's not easy to do this. This should be solved in the future with the support of
            // module annotations. For now, we only check deprecations because this is needed to correctly retire unneeded compiler arguments.
            val deprecationResolver =
                DeprecationResolver(LockBasedStorageManager("ExperimentalUsageChecker"), languageVersionSettings, CoroutineCompatibilitySupport.ENABLED, DeprecationSettings.Default)

            // Returns true if fqName refers to a valid experimental API marker.
            fun checkAnnotation(fqName: String): Boolean {
                val descriptor = module.resolveClassByFqName(FqName(fqName), NoLookupLocation.FOR_NON_TRACKED_SCOPE)
                if (descriptor == null) {
                    reportWarning("Experimental API marker $fqName is unresolved. Please make sure it's present in the module dependencies")
                    return false
                }

                if (descriptor.loadExperimentalityForMarkerAnnotation() == null) {
                    reportWarning("Class $fqName is not an experimental API marker annotation")
                    return false
                }

                for (deprecation in deprecationResolver.getDeprecations(descriptor)) {
                    val report = when (deprecation.deprecationLevel) {
                        DeprecationLevelValue.WARNING -> reportWarning
                        DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> reportError
                    }
                    report("Experimental API marker $fqName is deprecated" + deprecation.message?.let { ". $it" }.orEmpty())
                }
                return true
            }

            val validExperimental = languageVersionSettings.getFlag(AnalysisFlags.experimental).filter(::checkAnnotation)
            val validUseExperimental = languageVersionSettings.getFlag(AnalysisFlags.useExperimental).filter { fqName ->
                fqName == REQUIRES_OPT_IN_FQ_NAME.asString() || fqName == OLD_EXPERIMENTAL_FQ_NAME.asString() || checkAnnotation(fqName)
            }

            for (fqName in validExperimental.intersect(validUseExperimental)) {
                reportError("'-Xuse-experimental=$fqName' has no effect because '-Xexperimental=$fqName' is used")
            }
        }
    }

    class ClassifierUsage(project: Project) : ClassifierUsageChecker {
        private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

        override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
            val name = targetDescriptor.name
            if (name == OLD_EXPERIMENTAL_FQ_NAME.shortName() || name == REQUIRES_OPT_IN_FQ_NAME.shortName() ||
                name == OLD_USE_EXPERIMENTAL_FQ_NAME.shortName() || name == OPT_IN_FQ_NAME.shortName()) {
                val fqName = targetDescriptor.fqNameSafe
                if (fqName in EXPERIMENTAL_FQ_NAMES || fqName in USE_EXPERIMENTAL_FQ_NAMES) {
                    checkUsageOfKotlinExperimentalOrUseExperimental(element, context)
                    return
                }
            }

            val targetClass = when (targetDescriptor) {
                is ClassDescriptor -> targetDescriptor
                is TypeAliasDescriptor -> targetDescriptor.classDescriptor
                else -> null
            }
            if (targetClass != null && targetClass.loadExperimentalityForMarkerAnnotation() != null) {
                if (!element.isUsageAsAnnotationOrImport() &&
                    !element.isUsageAsUseExperimentalArgument(context.trace.bindingContext)
                ) {
                    context.trace.report(
                        Errors.EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL.on(element)
                    )
                }
            }

            if (element.getParentOfType<KtImportDirective>(false) == null) {
                val experimentalities = targetDescriptor.loadExperimentalities(moduleAnnotationsResolver, context.languageVersionSettings)
                reportNotAcceptedExperimentalities(experimentalities, element, context)
            }
        }

        private fun checkUsageOfKotlinExperimentalOrUseExperimental(element: PsiElement, context: CheckerContext) {
            val useExperimentalFqNames = context.languageVersionSettings.getFlag(AnalysisFlags.useExperimental)
            if (REQUIRES_OPT_IN_FQ_NAME.asString() !in useExperimentalFqNames &&
                OLD_EXPERIMENTAL_FQ_NAME.asString() !in useExperimentalFqNames) {
                context.trace.report(Errors.EXPERIMENTAL_IS_NOT_ENABLED.on(element))
            }

            if (!element.isUsageAsAnnotationOrImport() && !element.isUsageAsQualifier()) {
                context.trace.report(Errors.EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION.on(element))
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

        private fun PsiElement.isUsageAsUseExperimentalArgument(bindingContext: BindingContext): Boolean {
            val qualifier = (this as? KtSimpleNameExpression)?.getTopmostParentQualifiedExpressionForSelector() ?: this
            val parent = qualifier.parent

            return parent is KtClassLiteralExpression &&
                    parent.parent is KtValueArgument &&
                    parent.parent.parent is KtValueArgumentList &&
                    parent.parent.parent.parent.let { entry ->
                        entry is KtAnnotationEntry && bindingContext.get(BindingContext.ANNOTATION, entry)?.let { annotation ->
                            annotation.fqName in USE_EXPERIMENTAL_FQ_NAMES || annotation.fqName == WAS_EXPERIMENTAL_FQ_NAME
                        } == true
                    }
        }
    }

    class Overrides(project: Project) : DeclarationChecker {
        private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

        override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
            if (descriptor !is CallableMemberDescriptor) return

            val experimentalOverridden = descriptor.overriddenDescriptors.flatMap { member ->
                member.loadExperimentalities(moduleAnnotationsResolver, context.languageVersionSettings)
                    .map { experimentality -> experimentality to member }
            }.toMap()

            for ((experimentality, member) in experimentalOverridden) {
                if (!declaration.isExperimentalityAccepted(experimentality.annotationFqName, context)) {
                    val diagnostic = when (experimentality.severity) {
                        Experimentality.Severity.WARNING -> Errors.EXPERIMENTAL_OVERRIDE
                        Experimentality.Severity.ERROR -> Errors.EXPERIMENTAL_OVERRIDE_ERROR
                    }
                    val reportOn = (declaration as? KtNamedDeclaration)?.nameIdentifier ?: declaration
                    context.trace.report(diagnostic.on(reportOn, experimentality.annotationFqName, member.containingDeclaration))
                }
            }
        }
    }
}
