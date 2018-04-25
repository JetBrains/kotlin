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
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull

class ExperimentalUsageChecker(project: Project) : CallChecker {
    private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

    internal data class Experimentality(val annotationFqName: FqName, val severity: Severity) {
        enum class Severity { WARNING, ERROR }

        companion object {
            val DEFAULT_SEVERITY = Severity.ERROR
        }
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        checkExperimental(resolvedCall.resultingDescriptor, reportOn, context, moduleAnnotationsResolver)
    }

    companion object {
        val EXPERIMENTAL_FQ_NAME = FqName("kotlin.Experimental")
        internal val USE_EXPERIMENTAL_FQ_NAME = FqName("kotlin.UseExperimental")
        internal val USE_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")

        private val LEVEL = Name.identifier("level")
        private val WARNING_LEVEL = Name.identifier("WARNING")
        private val ERROR_LEVEL = Name.identifier("ERROR")

        private fun checkExperimental(
            descriptor: DeclarationDescriptor,
            element: PsiElement,
            context: CheckerContext,
            moduleAnnotationsResolver: ModuleAnnotationsResolver
        ) {
            val experimentalities = descriptor.loadExperimentalities(moduleAnnotationsResolver)
            if (experimentalities.isNotEmpty()) {
                checkExperimental(
                    experimentalities, element, context.trace.bindingContext, context.languageVersionSettings
                ) { (annotationFqName, severity) ->
                    val diagnostic = when (severity) {
                        Experimentality.Severity.WARNING -> Errors.EXPERIMENTAL_API_USAGE
                        Experimentality.Severity.ERROR -> Errors.EXPERIMENTAL_API_USAGE_ERROR
                    }
                    context.trace.report(diagnostic.on(element, annotationFqName))
                }
            }
        }

        private fun checkExperimental(
            experimentalities: Collection<Experimentality>,
            element: PsiElement,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings,
            report: (Experimentality) -> Unit
        ) {
            for (experimentality in experimentalities) {
                val annotationFqName = experimentality.annotationFqName
                val isExperimentalityAccepted =
                    element.hasContainerAnnotatedWithUseExperimental(
                        annotationFqName, bindingContext, languageVersionSettings
                    ) || element.propagates(
                        annotationFqName, bindingContext, languageVersionSettings
                    )

                if (!isExperimentalityAccepted) {
                    report(experimentality)
                }
            }
        }

        private fun DeclarationDescriptor.loadExperimentalities(
            moduleAnnotationsResolver: ModuleAnnotationsResolver
        ): Set<Experimentality> {
            val result = SmartSet.create<Experimentality>()

            for (annotation in annotations) {
                result.addIfNotNull(annotation.annotationClass?.loadExperimentalityForMarkerAnnotation())
            }

            val container = containingDeclaration
            if (container is ClassDescriptor && this !is ConstructorDescriptor) {
                result.addAll(container.loadExperimentalities(moduleAnnotationsResolver))
            }

            for (moduleAnnotationClassId in moduleAnnotationsResolver.getAnnotationsOnContainingModule(this)) {
                val annotationClass = module.findClassAcrossModuleDependencies(moduleAnnotationClassId)
                result.addIfNotNull(annotationClass?.loadExperimentalityForMarkerAnnotation())
            }

            return result
        }

        internal fun ClassDescriptor.loadExperimentalityForMarkerAnnotation(): Experimentality? {
            val experimental = annotations.findAnnotation(EXPERIMENTAL_FQ_NAME) ?: return null

            val severity = when ((experimental.allValueArguments[LEVEL] as? EnumValue)?.enumEntryName) {
                WARNING_LEVEL -> Experimentality.Severity.WARNING
                ERROR_LEVEL -> Experimentality.Severity.ERROR
                else -> Experimentality.DEFAULT_SEVERITY
            }

            return Experimentality(fqNameSafe, severity)
        }

        // Checks whether any of the non-local enclosing declarations is annotated with annotationFqName, effectively requiring
        // propagation for the experimental annotation to the call sites
        private fun PsiElement.propagates(
            annotationFqName: FqName,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
        ): Boolean =
            annotationFqName.asString() in languageVersionSettings.getFlag(AnalysisFlag.experimental) ||
                    anyParentMatches { element, _ ->
                        if (element is KtDeclaration) {
                            val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
                            descriptor != null && !DescriptorUtils.isLocal(descriptor) &&
                                    descriptor.annotations.hasAnnotation(annotationFqName)
                        } else false
                    }

        // Checks whether there's an element lexically above the tree, that is annotated with `@UseExperimental(X::class)`
        // where annotationFqName is the FQ name of X
        private fun PsiElement.hasContainerAnnotatedWithUseExperimental(
            annotationFqName: FqName,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
        ): Boolean =
            annotationFqName.asString() in languageVersionSettings.getFlag(AnalysisFlag.useExperimental) ||
                    anyParentMatches { element, _ ->
                        element is KtAnnotated && element.annotationEntries.any { entry ->
                            bindingContext.get(BindingContext.ANNOTATION, entry)?.isUseExperimental(annotationFqName) == true
                        }
                    }

        private inline fun PsiElement.anyParentMatches(predicate: (element: PsiElement, parent: PsiElement?) -> Boolean): Boolean {
            var element = this
            while (true) {
                val parent = element.parent
                if (predicate(element, parent)) return true
                element = parent ?: return false
            }
        }

        private fun AnnotationDescriptor.isUseExperimental(annotationFqName: FqName): Boolean {
            if (fqName != USE_EXPERIMENTAL_FQ_NAME) return false

            val annotationClasses = allValueArguments[USE_EXPERIMENTAL_ANNOTATION_CLASS]
            return annotationClasses is ArrayValue && annotationClasses.value.any { annotationClass ->
                (annotationClass as? KClassValue)?.value?.constructor?.declarationDescriptor?.fqNameSafe == annotationFqName
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
            val deprecationResolver = DeprecationResolver(LockBasedStorageManager(), languageVersionSettings)

            fun checkAnnotation(fqName: String): Boolean {
                val descriptor = module.resolveClassByFqName(FqName(fqName), NoLookupLocation.FOR_NON_TRACKED_SCOPE)
                val experimentality = descriptor?.loadExperimentalityForMarkerAnnotation()
                val message = when {
                    descriptor == null ->
                        "Experimental API marker $fqName is unresolved. Please make sure it's present in the module dependencies"
                    experimentality == null ->
                        "Class $fqName is not an experimental API marker annotation"
                    else -> {
                        for (deprecation in deprecationResolver.getDeprecations(descriptor)) {
                            val report = when (deprecation.deprecationLevel) {
                                DeprecationLevelValue.WARNING -> reportWarning
                                DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> reportError
                            }
                            report("Experimental API marker $fqName is deprecated" + deprecation.message?.let { ". $it" }.orEmpty())
                        }
                        return true
                    }
                }

                reportError(message)

                return false
            }

            val validExperimental = languageVersionSettings.getFlag(AnalysisFlag.experimental).filter(::checkAnnotation)
            val validUseExperimental = languageVersionSettings.getFlag(AnalysisFlag.useExperimental).filter(::checkAnnotation)

            for (fqName in validExperimental.intersect(validUseExperimental)) {
                reportError("'-Xuse-experimental=$fqName' has no effect because '-Xexperimental=$fqName' is used")
            }
        }
    }

    class ClassifierUsage(project: Project) : ClassifierUsageChecker {
        private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

        override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
            checkExperimental(targetDescriptor, element, context, moduleAnnotationsResolver)
        }
    }

    class Overrides(project: Project) : DeclarationChecker {
        private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

        override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
            if (descriptor !is CallableMemberDescriptor) return

            val experimentalOverridden = descriptor.overriddenDescriptors.flatMap { member ->
                member.loadExperimentalities(moduleAnnotationsResolver).map { experimentality -> experimentality to member }
            }.toMap()

            for ((experimentality, member) in experimentalOverridden) {
                checkExperimental(
                    listOf(experimentality), declaration, context.trace.bindingContext, context.languageVersionSettings
                ) { (annotationFqName, severity) ->
                    val diagnostic = when (severity) {
                        Experimentality.Severity.WARNING -> Errors.EXPERIMENTAL_OVERRIDE
                        Experimentality.Severity.ERROR -> Errors.EXPERIMENTAL_OVERRIDE_ERROR
                    }
                    val reportOn = (declaration as? KtNamedDeclaration)?.nameIdentifier ?: declaration
                    context.trace.report(diagnostic.on(reportOn, annotationFqName, member.containingDeclaration))
                }
            }
        }
    }
}
