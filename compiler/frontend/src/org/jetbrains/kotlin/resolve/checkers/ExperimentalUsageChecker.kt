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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
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
        val experimentalities =
            resolvedCall.resultingDescriptor.loadExperimentalities(moduleAnnotationsResolver, context.languageVersionSettings)
        reportNotAcceptedExperimentalities(experimentalities, reportOn, context)
    }

    companion object {
        val EXPERIMENTAL_FQ_NAME = FqName("kotlin.Experimental")
        internal val USE_EXPERIMENTAL_FQ_NAME = FqName("kotlin.UseExperimental")
        internal val WAS_EXPERIMENTAL_FQ_NAME = FqName("kotlin.WasExperimental")
        internal val USE_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")
        internal val WAS_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")

        private val LEVEL = Name.identifier("level")
        private val WARNING_LEVEL = Name.identifier("WARNING")
        private val ERROR_LEVEL = Name.identifier("ERROR")

        private val EXPERIMENTAL_SHORT_NAME = EXPERIMENTAL_FQ_NAME.shortName()
        private val USE_EXPERIMENTAL_SHORT_NAME = USE_EXPERIMENTAL_FQ_NAME.shortName()

        private fun reportNotAcceptedExperimentalities(
            experimentalities: Collection<Experimentality>, element: PsiElement, context: CheckerContext
        ) {
            for ((annotationFqName, severity) in experimentalities) {
                if (!element.isExperimentalityAccepted(annotationFqName, context)) {
                    val diagnostic = when (severity) {
                        Experimentality.Severity.WARNING -> Errors.EXPERIMENTAL_API_USAGE
                        Experimentality.Severity.ERROR -> Errors.EXPERIMENTAL_API_USAGE_ERROR
                    }
                    context.trace.report(diagnostic.on(element, annotationFqName))
                }
            }
        }

        private fun DeclarationDescriptor.loadExperimentalities(
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
            val experimental = annotations.findAnnotation(EXPERIMENTAL_FQ_NAME) ?: return null

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
            annotationFqName.asString() in languageVersionSettings.getFlag(AnalysisFlag.experimental) ||
                    annotationFqName.asString() in languageVersionSettings.getFlag(AnalysisFlag.useExperimental) ||
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
                if (descriptor?.fqName == USE_EXPERIMENTAL_FQ_NAME) {
                    val annotationClasses = descriptor.allValueArguments[USE_EXPERIMENTAL_ANNOTATION_CLASS]
                    annotationClasses is ArrayValue && annotationClasses.value.any { annotationClass ->
                        (annotationClass as? KClassValue)?.value?.constructor?.declarationDescriptor?.fqNameSafe == annotationFqName
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
            val validUseExperimental = languageVersionSettings.getFlag(AnalysisFlag.useExperimental).filter { fqName ->
                fqName == EXPERIMENTAL_FQ_NAME.asString() || checkAnnotation(fqName)
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
            if (name == EXPERIMENTAL_SHORT_NAME || name == USE_EXPERIMENTAL_SHORT_NAME) {
                val fqName = targetDescriptor.fqNameUnsafe
                if (fqName == EXPERIMENTAL_FQ_NAME.toUnsafe() || fqName == USE_EXPERIMENTAL_FQ_NAME.toUnsafe()) {
                    checkUsageOfKotlinExperimentalOrUseExperimental(element, context)
                    return
                }
            }

            val experimentalities = targetDescriptor.loadExperimentalities(moduleAnnotationsResolver, context.languageVersionSettings)
            reportNotAcceptedExperimentalities(experimentalities, element, context)

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
        }

        private fun checkUsageOfKotlinExperimentalOrUseExperimental(element: PsiElement, context: CheckerContext) {
            if (EXPERIMENTAL_FQ_NAME.asString() !in context.languageVersionSettings.getFlag(AnalysisFlag.useExperimental)) {
                context.trace.report(Errors.EXPERIMENTAL_IS_NOT_ENABLED.on(element))
            }

            if (!element.isUsageAsAnnotationOrImport() && !element.isUsageAsQualifier()) {
                context.trace.report(Errors.EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION.on(element))
            }
        }

        private fun PsiElement.isUsageAsAnnotationOrImport(): Boolean {
            val parent = parent

            if (parent is KtUserType) {
                return parent.parent is KtTypeReference &&
                        parent.parent.parent is KtConstructorCalleeExpression &&
                        parent.parent.parent.parent is KtAnnotationEntry
            }

            return parent is KtDotQualifiedExpression && parent.parent is KtImportDirective
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

        private fun PsiElement.isUsageAsUseExperimentalArgument(bindingContext: BindingContext): Boolean =
            parent is KtClassLiteralExpression &&
                    parent.parent is KtValueArgument &&
                    parent.parent.parent is KtValueArgumentList &&
                    parent.parent.parent.parent.let { entry ->
                        entry is KtAnnotationEntry && bindingContext.get(BindingContext.ANNOTATION, entry)?.let { annotation ->
                            annotation.fqName == USE_EXPERIMENTAL_FQ_NAME || annotation.fqName == WAS_EXPERIMENTAL_FQ_NAME
                        } == true
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
