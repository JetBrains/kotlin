/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.injection

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.patterns.PatternConditionPlus
import com.intellij.patterns.PsiClassNamePatternCondition
import com.intellij.patterns.ValuePatternCondition
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.intelliLang.Configuration
import org.intellij.plugins.intelliLang.inject.InjectorUtils
import org.intellij.plugins.intelliLang.inject.LanguageInjectionSupport
import org.intellij.plugins.intelliLang.inject.TemporaryPlacesRegistry
import org.intellij.plugins.intelliLang.inject.config.BaseInjection
import org.intellij.plugins.intelliLang.inject.config.InjectionPlace
import org.intellij.plugins.intelliLang.inject.java.JavaLanguageInjectionSupport
import org.intellij.plugins.intelliLang.util.AnnotationUtilEx
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.runInReadActionWithWriteActionPriority
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.util.aliasImportMap
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinLanguageInjector(
        private val configuration: Configuration,
        private val project: Project,
        private val temporaryPlacesRegistry: TemporaryPlacesRegistry
) : MultiHostInjector {
    companion object {
        private val STRING_LITERALS_REGEXP = "\"([^\"]*)\"".toRegex()
        private val ABSENT_KOTLIN_INJECTION = BaseInjection("ABSENT_KOTLIN_BASE_INJECTION")
    }

    private val kotlinSupport: KotlinLanguageInjectionSupport? by lazy {
        ArrayList(InjectorUtils.getActiveInjectionSupports()).filterIsInstance(KotlinLanguageInjectionSupport::class.java).firstOrNull()
    }

    private data class KotlinCachedInjection(val modificationCount: Long, val baseInjection: BaseInjection)

    private var KtStringTemplateExpression.cachedInjectionWithModification: KotlinCachedInjection? by UserDataProperty(
            Key.create<KotlinCachedInjection>("CACHED_INJECTION_WITH_MODIFICATION"))

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val ktHost: KtStringTemplateExpression = context as? KtStringTemplateExpression ?: return
        if (!context.isValidHost) return

        val support = kotlinSupport ?: return

        if (!ProjectRootsUtil.isInProjectOrLibSource(ktHost.containingFile.originalFile)) return

        val needImmediateAnswer = with(ApplicationManager.getApplication()) { isDispatchThread && !isUnitTestMode }
        val kotlinCachedInjection = ktHost.cachedInjectionWithModification
        val modificationCount = PsiManager.getInstance(project).modificationTracker.modificationCount

        val baseInjection = when {
            needImmediateAnswer -> {
                // Can't afford long counting or typing will be laggy. Force cache reuse even if it's outdated.
                kotlinCachedInjection?.baseInjection ?: ABSENT_KOTLIN_INJECTION
            }
            kotlinCachedInjection != null && (modificationCount == kotlinCachedInjection.modificationCount) ->
                // Cache is up-to-date
                kotlinCachedInjection.baseInjection
            else -> {
                fun computeAndCache(): BaseInjection {
                    val computedInjection = computeBaseInjection(ktHost, support, registrar) ?: ABSENT_KOTLIN_INJECTION
                    ktHost.cachedInjectionWithModification = KotlinCachedInjection(modificationCount, computedInjection)
                    return computedInjection
                }

                if (ApplicationManager.getApplication().isReadAccessAllowed && ProgressManager.getInstance().progressIndicator == null) {
                    // The action cannot be canceled by caller and by internal checkCanceled() calls.
                    // Force creating new indicator that is canceled on write action start, otherwise there might be lags in typing.
                    runInReadActionWithWriteActionPriority(::computeAndCache) ?: kotlinCachedInjection?.baseInjection ?: ABSENT_KOTLIN_INJECTION
                }
                else {
                    computeAndCache()
                }
            }
        }

        if (baseInjection == ABSENT_KOTLIN_INJECTION) {
            return
        }

        val language = InjectorUtils.getLanguageByString(baseInjection.injectedLanguageId) ?: return

        if (ktHost.hasInterpolation()) {
            val file = ktHost.containingKtFile
            val parts = splitLiteralToInjectionParts(baseInjection, ktHost) ?: return

            if (parts.ranges.isEmpty()) return

            InjectorUtils.registerInjection(language, parts.ranges, file, registrar)
            InjectorUtils.registerSupport(support, false, registrar)
            InjectorUtils.putInjectedFileUserData(registrar, InjectedLanguageUtil.FRANKENSTEIN_INJECTION,
                                                  if (parts.isUnparsable) java.lang.Boolean.TRUE else null)
        }
        else {
            InjectorUtils.registerInjectionSimple(ktHost, baseInjection, support, registrar)
        }
    }

    @Suppress("FoldInitializerAndIfToElvis")
    private fun computeBaseInjection(
            ktHost: KtStringTemplateExpression,
            support: KotlinLanguageInjectionSupport,
            registrar: MultiHostRegistrar): BaseInjection? {
        val containingFile = ktHost.containingFile

        val tempInjectedLanguage = temporaryPlacesRegistry.getLanguageFor(ktHost, containingFile)
        if (tempInjectedLanguage != null) {
            InjectorUtils.putInjectedFileUserData(registrar, LanguageInjectionSupport.TEMPORARY_INJECTED_LANGUAGE, tempInjectedLanguage)
            return BaseInjection(support.id).apply {
                injectedLanguageId = tempInjectedLanguage.id
                prefix = tempInjectedLanguage.prefix
                suffix = tempInjectedLanguage.suffix
            }
        }

        return findInjectionInfo(ktHost)?.toBaseInjection(support)
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(KtStringTemplateExpression::class.java)
    }

    private fun findInjectionInfo(place: KtElement, originalHost: Boolean = true): InjectionInfo? {
        return injectWithExplicitCodeInstruction(place)
               ?: injectWithCall(place)
               ?: injectInAnnotationCall(place)
               ?: injectWithReceiver(place)
               ?: injectWithVariableUsage(place, originalHost)
    }

    private fun injectWithExplicitCodeInstruction(host: KtElement): InjectionInfo? {
        val support = kotlinSupport ?: return null
        val languageId =
                support.findInjectionCommentLanguageId(host) ?:
                support.findAnnotationInjectionLanguageId(host) ?:
                return null
        return InjectionInfo(languageId, null, null)
    }

    private fun injectWithReceiver(host: KtElement): InjectionInfo? {
        val qualifiedExpression = host.parent as? KtDotQualifiedExpression ?: return null
        if (qualifiedExpression.receiverExpression != host) return null

        val callExpression = qualifiedExpression.selectorExpression as? KtCallExpression ?: return null
        val callee = callExpression.calleeExpression ?: return null

        if (isAnalyzeOff()) return null

        val kotlinInjections = configuration.getInjections(KOTLIN_SUPPORT_ID)

        val calleeName = callee.text
        val possibleNames = collectPossibleNames(kotlinInjections)

        if (calleeName !in possibleNames) {
            return null
        }

        for (reference in callee.references) {
            ProgressManager.checkCanceled()

            val resolvedTo = reference.resolve()
            if (resolvedTo is KtFunction) {
                val injectionInfo = findInjection(resolvedTo.receiverTypeReference, kotlinInjections)
                if (injectionInfo != null) {
                    return injectionInfo
                }
            }
        }

        return null
    }

    private fun collectPossibleNames(injections: List<BaseInjection>): Set<String> {
        val result = HashSet<String>()

        for (injection in injections) {
            val injectionPlaces = injection.injectionPlaces
            for (place in injectionPlaces) {
                val placeStr = place.toString()
                val literals = STRING_LITERALS_REGEXP.findAll(placeStr).map { it.groupValues[1] }
                result.addAll(literals)
            }
        }

        return result
    }

    private fun injectWithVariableUsage(host: KtElement, originalHost: Boolean): InjectionInfo? {
        // Given place is not original host of the injection so we stop to prevent stepping through indirect references
        if (!originalHost) return null

        val ktProperty = host.parent as? KtProperty ?: return null
        if (ktProperty.initializer != host) return null

        if (isAnalyzeOff()) return null

        val searchScope = LocalSearchScope(arrayOf(ktProperty.containingFile), "", true)
        return ReferencesSearch.search(ktProperty, searchScope).asSequence().mapNotNull { psiReference ->
            val element = psiReference.element as? KtElement ?: return@mapNotNull null
            findInjectionInfo(element, false)
        }.firstOrNull()
    }

    private fun injectWithCall(host: KtElement): InjectionInfo? {
        val ktHost: KtElement = host
        val argument = ktHost.parent as? KtValueArgument ?: return null

        val callExpression = PsiTreeUtil.getParentOfType(ktHost, KtCallExpression::class.java) ?: return null
        val callee = callExpression.calleeExpression ?: return null

        if (isAnalyzeOff()) return null

        for (reference in callee.references) {
            ProgressManager.checkCanceled()

            val resolvedTo = reference.resolve()
            if (resolvedTo is PsiMethod) {
                val injectionForJavaMethod = injectionForJavaMethod(argument, resolvedTo)
                if (injectionForJavaMethod != null) {
                    return injectionForJavaMethod
                }
            }
            else if (resolvedTo is KtFunction) {
                val injectionForJavaMethod = injectionForKotlinCall(argument, resolvedTo, reference)
                if (injectionForJavaMethod != null) {
                    return injectionForJavaMethod
                }
            }
        }

        return null
    }

    private fun injectInAnnotationCall(host: KtElement): InjectionInfo? {
        val argument = host.parent as? KtValueArgument ?: return null
        val annotationEntry = argument.parent.parent as? KtAnnotationEntry ?: return null
        if (!fastCheckInjectionsExists(annotationEntry)) return null
        val callDescriptor = annotationEntry.getResolvedCall(annotationEntry.analyze())?.candidateDescriptor
        val psiClass = (callDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() as? PsiClass ?: return null
        val argumentName = argument.getArgumentName()?.asName?.identifier ?: "value"
        val method = psiClass.findMethodsByName(argumentName, false).singleOrNull() ?: return null
        return findInjection(method, configuration.getInjections(JavaLanguageInjectionSupport.JAVA_SUPPORT_ID))
    }

    private fun injectionForJavaMethod(argument: KtValueArgument, javaMethod: PsiMethod): InjectionInfo? {
        val argumentIndex = (argument.parent as KtValueArgumentList).arguments.indexOf(argument)
        val psiParameter = javaMethod.parameterList.parameters.getOrNull(argumentIndex) ?: return null

        val injectionInfo = findInjection(psiParameter, configuration.getInjections(JavaLanguageInjectionSupport.JAVA_SUPPORT_ID))
        if (injectionInfo != null) {
            return injectionInfo
        }

        val annotations = AnnotationUtilEx.getAnnotationFrom(
                psiParameter,
                configuration.advancedConfiguration.languageAnnotationPair,
                true)

        if (annotations.isNotEmpty()) {
            return processAnnotationInjectionInner(annotations)
        }

        return null
    }

    private fun injectionForKotlinCall(argument: KtValueArgument, ktFunction: KtFunction, reference: PsiReference): InjectionInfo? {
        val argumentIndex = (argument.parent as KtValueArgumentList).arguments.indexOf(argument)
        val ktParameter = ktFunction.valueParameters.getOrNull(argumentIndex) ?: return null

        val patternInjection = findInjection(ktParameter, configuration.getInjections(KOTLIN_SUPPORT_ID))
        if (patternInjection != null) {
            return patternInjection
        }

        // Found psi element after resolve can be obtained from compiled declaration but annotations parameters are lost there.
        // Search for original descriptor from reference.
        val ktReference = reference as? KtReference ?: return null
        val bindingContext = ktReference.element.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        val functionDescriptor = ktReference.resolveToDescriptors(bindingContext).singleOrNull() as? FunctionDescriptor ?: return null

        val parameterDescriptor = functionDescriptor.valueParameters.getOrNull(argumentIndex) ?: return null
        val injectAnnotation = parameterDescriptor.annotations.findAnnotation(FqName(AnnotationUtil.LANGUAGE)) ?: return null

        val languageId = injectAnnotation.argumentValue("value") as? String ?: return null
        return InjectionInfo(languageId, null, null)
    }

    private fun findInjection(element: PsiElement?, injections: List<BaseInjection>): InjectionInfo? {
        for (injection in injections) {
            if (injection.acceptsPsiElement(element)) {
                return InjectionInfo(injection.injectedLanguageId, injection.prefix, injection.suffix)
            }
        }

        return null
    }

    private fun isAnalyzeOff(): Boolean {
        return configuration.advancedConfiguration.dfaOption == Configuration.DfaOption.OFF
    }

    private class InjectionInfo(val languageId: String?, val prefix: String?, val suffix: String?) {
        fun toBaseInjection(injectionSupport: KotlinLanguageInjectionSupport): BaseInjection? {
            if (languageId == null) return null

            val baseInjection = BaseInjection(injectionSupport.id)
            baseInjection.injectedLanguageId = languageId

            if (prefix != null) {
                baseInjection.prefix = prefix
            }

            if (suffix != null) {
                baseInjection.suffix = suffix
            }

            return baseInjection
        }
    }

    private fun processAnnotationInjectionInner(annotations: Array<PsiAnnotation>): InjectionInfo? {
        val id = AnnotationUtilEx.calcAnnotationValue(annotations, "value")
        val prefix = AnnotationUtilEx.calcAnnotationValue(annotations, "prefix")
        val suffix = AnnotationUtilEx.calcAnnotationValue(annotations, "suffix")

        return InjectionInfo(id, prefix, suffix)
    }

    private val injectableTargetClassShortNames = CachedValuesManager.getManager(project)
            .createCachedValue({
                                   CachedValueProvider.Result.create(HashSet<String>().apply {
                                       for (injection in configuration.getInjections(JavaLanguageInjectionSupport.JAVA_SUPPORT_ID)) {
                                           for (injectionPlace in injection.injectionPlaces) {
                                               for (targetClassFQN in retrievePlaceTargetClassesFQNs(injectionPlace)) {
                                                   add(StringUtilRt.getShortName(targetClassFQN))
                                               }
                                           }
                                       }
                                   }, configuration)
                               }, false)

    private fun fastCheckInjectionsExists(annotationEntry: KtAnnotationEntry): Boolean {
        val referencedName = (annotationEntry.typeReference?.typeElement as? KtUserType)?.referencedName ?: return false
        val annotationShortName = annotationEntry.containingKtFile.aliasImportMap()[referencedName].singleOrNull() ?: referencedName
        return annotationShortName in injectableTargetClassShortNames.value
    }

    private fun retrievePlaceTargetClassesFQNs(place: InjectionPlace): Collection<String> {
        val classCondition = place.elementPattern.condition.conditions.firstOrNull { it.debugMethodName == "definedInClass" }
                                     as? PatternConditionPlus<*, *> ?: return emptyList()
        val psiClassNamePatternCondition = classCondition.valuePattern.condition.conditions.
                firstIsInstanceOrNull<PsiClassNamePatternCondition>() ?: return emptyList()
        val valuePatternCondition = psiClassNamePatternCondition.namePattern.condition.conditions.firstIsInstanceOrNull<ValuePatternCondition<String>>() ?: return emptyList()
        return valuePatternCondition.values
    }

}