/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.injection

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PatternConditionPlus
import com.intellij.patterns.PsiClassNamePatternCondition
import com.intellij.patterns.ValuePatternCondition
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.intelliLang.Configuration
import org.intellij.plugins.intelliLang.inject.InjectorUtils
import org.intellij.plugins.intelliLang.inject.TemporaryPlacesRegistry
import org.intellij.plugins.intelliLang.inject.config.BaseInjection
import org.intellij.plugins.intelliLang.inject.config.InjectionPlace
import org.intellij.plugins.intelliLang.inject.java.JavaLanguageInjectionSupport
import org.intellij.plugins.intelliLang.util.AnnotationUtilEx
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.idea.caches.resolve.allowResolveInDispatchThread
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.runInReadActionWithWriteActionPriority
import org.jetbrains.kotlin.idea.patterns.KotlinFunctionPattern
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveToDescriptors
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.aliasImportMap
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinLanguageInjector(
    private val project: Project
) : MultiHostInjector {
    private val configuration get() = Configuration.getProjectInstance(project)

    companion object {
        private val STRING_LITERALS_REGEXP = "\"([^\"]*)\"".toRegex()
        private val ABSENT_KOTLIN_INJECTION = BaseInjection("ABSENT_KOTLIN_BASE_INJECTION")
    }

    private val kotlinSupport: KotlinLanguageInjectionSupport? by lazy {
        ArrayList(InjectorUtils.getActiveInjectionSupports()).filterIsInstance(KotlinLanguageInjectionSupport::class.java).firstOrNull()
    }

    private data class KotlinCachedInjection(val modificationCount: Long, val baseInjection: BaseInjection)

    private var KtStringTemplateExpression.cachedInjectionWithModification: KotlinCachedInjection? by UserDataProperty(
        Key.create<KotlinCachedInjection>("CACHED_INJECTION_WITH_MODIFICATION")
    )

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
                    val computedInjection = computeBaseInjection(ktHost, support) ?: ABSENT_KOTLIN_INJECTION
                    ktHost.cachedInjectionWithModification = KotlinCachedInjection(modificationCount, computedInjection)
                    return computedInjection
                }

                if (ApplicationManager.getApplication().isReadAccessAllowed && ProgressManager.getInstance().progressIndicator == null) {
                    // The action cannot be canceled by caller and by internal checkCanceled() calls.
                    // Force creating new indicator that is canceled on write action start, otherwise there might be lags in typing.
                    runInReadActionWithWriteActionPriority(::computeAndCache) ?: kotlinCachedInjection?.baseInjection
                    ?: ABSENT_KOTLIN_INJECTION
                } else {
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
            InjectorUtils.registerSupport(support, false, ktHost, language)
            InjectorUtils.putInjectedFileUserData(
                ktHost,
                language,
                InjectedLanguageManager.FRANKENSTEIN_INJECTION,
                if (parts.isUnparsable) java.lang.Boolean.TRUE else null
            )
        } else {
            InjectorUtils.registerInjectionSimple(ktHost, baseInjection, support, registrar)
        }
    }

    @Suppress("FoldInitializerAndIfToElvis")
    private fun computeBaseInjection(
        ktHost: KtStringTemplateExpression,
        support: KotlinLanguageInjectionSupport
    ): BaseInjection? {
        val containingFile = ktHost.containingFile

        val tempInjectedLanguage = TemporaryPlacesRegistry.getInstance(project).getLanguageFor(ktHost, containingFile)
        if (tempInjectedLanguage != null) {
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
            ?: injectReturnValue(place)
            ?: injectInAnnotationCall(place)
            ?: injectWithReceiver(place)
            ?: injectWithVariableUsage(place, originalHost)
    }

    private fun injectReturnValue(place: KtElement): InjectionInfo? {
        val parent = place.parent

        tailrec fun findReturnExpression(expression: PsiElement?): KtReturnExpression? = when (expression) {
            is KtReturnExpression -> expression
            is KtBinaryExpression -> findReturnExpression(expression.takeIf { it.operationToken == KtTokens.ELVIS }?.parent)
            is KtContainerNodeForControlStructureBody, is KtIfExpression -> findReturnExpression(expression.parent)
            else -> null
        }

        val returnExp = findReturnExpression(parent) ?: return null

        if (returnExp.labeledExpression != null) return null

        val callableDeclaration = PsiTreeUtil.getParentOfType(returnExp, KtDeclaration::class.java) as? KtCallableDeclaration ?: return null
        if (callableDeclaration.annotationEntries.isEmpty()) return null

        val descriptor = callableDeclaration.descriptor ?: return null
        return injectionInfoByAnnotation(descriptor)
    }

    private fun injectWithExplicitCodeInstruction(host: KtElement): InjectionInfo? {
        val support = kotlinSupport ?: return null
        return InjectionInfo.fromBaseInjection(support.findCommentInjection(host)) ?: support.findAnnotationInjectionLanguageId(host)
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

    private tailrec fun injectWithCall(host: KtElement): InjectionInfo? {
        val argument = getArgument(host) ?: return null
        val callExpression = PsiTreeUtil.getParentOfType(argument, KtCallElement::class.java) ?: return null

        if (getCallableShortName(callExpression) == "arrayOf") return injectWithCall(callExpression)
        val callee = getNameReference(callExpression.calleeExpression) ?: return null

        if (isAnalyzeOff()) return null

        for (reference in callee.references) {
            ProgressManager.checkCanceled()

            val resolvedTo = allowResolveInDispatchThread { reference.resolve() }
            if (resolvedTo is PsiMethod) {
                val injectionForJavaMethod = injectionForJavaMethod(argument, resolvedTo)
                if (injectionForJavaMethod != null) {
                    return injectionForJavaMethod
                }
            } else if (resolvedTo is KtFunction) {
                val injectionForJavaMethod = injectionForKotlinCall(argument, resolvedTo, reference)
                if (injectionForJavaMethod != null) {
                    return injectionForJavaMethod
                }
            }
        }

        return null
    }

    private fun getNameReference(callee: KtExpression?): KtNameReferenceExpression? {
        if (callee is KtConstructorCalleeExpression)
            return callee.constructorReferenceExpression as? KtNameReferenceExpression
        return callee as? KtNameReferenceExpression
    }

    private fun getArgument(host: KtElement): KtValueArgument? = when (val parent = host.parent) {
        is KtValueArgument -> parent
        is KtCollectionLiteralExpression, is KtCallElement -> parent.parent as? KtValueArgument
        else -> null
    }

    private tailrec fun injectInAnnotationCall(host: KtElement): InjectionInfo? {
        val argument = getArgument(host) ?: return null
        val annotationEntry = argument.parent.parent as? KtCallElement ?: return null

        val callableShortName = getCallableShortName(annotationEntry) ?: return null
        if (callableShortName == "arrayOf") return injectInAnnotationCall(annotationEntry)

        if (!fastCheckInjectionsExists(callableShortName)) return null

        val calleeExpression = annotationEntry.calleeExpression ?: return null
        val callee = getNameReference(calleeExpression)?.mainReference?.let { reference ->
            allowResolveInDispatchThread { reference.resolve() }
        }
        when (callee) {
            is PsiClass -> {
                val psiClass = callee as? PsiClass ?: return null
                val argumentName = argument.getArgumentName()?.asName?.identifier ?: "value"
                val method = psiClass.findMethodsByName(argumentName, false).singleOrNull() ?: return null
                return findInjection(method, configuration.getInjections(JavaLanguageInjectionSupport.JAVA_SUPPORT_ID))
            }
            else -> return null
        }

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
            true
        )

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
        val functionDescriptor = allowResolveInDispatchThread {
            val bindingContext = ktReference.element.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            ktReference.resolveToDescriptors(bindingContext).singleOrNull() as? FunctionDescriptor
        } ?: return null

        val parameterDescriptor = functionDescriptor.valueParameters.getOrNull(argumentIndex) ?: return null
        return injectionInfoByAnnotation(parameterDescriptor)
    }

    private fun injectionInfoByAnnotation(annotated: Annotated): InjectionInfo? {
        val injectAnnotation = annotated.annotations.findAnnotation(FqName(AnnotationUtil.LANGUAGE)) ?: return null

        val languageId = injectAnnotation.argumentValue("value")?.safeAs<StringValue>()?.value ?: return null
        val prefix = injectAnnotation.argumentValue("prefix")?.safeAs<StringValue>()?.value
        val suffix = injectAnnotation.argumentValue("suffix")?.safeAs<StringValue>()?.value

        return InjectionInfo(languageId, prefix, suffix)
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

    private fun processAnnotationInjectionInner(annotations: Array<PsiAnnotation>): InjectionInfo? {
        val id = AnnotationUtilEx.calcAnnotationValue(annotations, "value")
        val prefix = AnnotationUtilEx.calcAnnotationValue(annotations, "prefix")
        val suffix = AnnotationUtilEx.calcAnnotationValue(annotations, "suffix")

        return InjectionInfo(id, prefix, suffix)
    }

    private fun createCachedValue(): CachedValueProvider.Result<HashSet<String>>? = with(configuration) {
        CachedValueProvider.Result.create(
            (getInjections(JavaLanguageInjectionSupport.JAVA_SUPPORT_ID) + getInjections(KOTLIN_SUPPORT_ID))
                .asSequence()
                .flatMap { it.injectionPlaces.asSequence() }
                .flatMap { retrieveJavaPlaceTargetClassesFQNs(it).asSequence() + retrieveKotlinPlaceTargetClassesFQNs(it).asSequence() }
                .map { StringUtilRt.getShortName(it) }
                .toHashSet()
            , this)
    }

    private val injectableTargetClassShortNames = CachedValuesManager.getManager(project).createCachedValue(::createCachedValue, false)

    private fun fastCheckInjectionsExists(annotationShortName: String) = annotationShortName in injectableTargetClassShortNames.value

    private fun getCallableShortName(annotationEntry: KtCallElement): String? {
        val referencedName = getNameReference(annotationEntry.calleeExpression)?.getReferencedName() ?: return null
        return annotationEntry.containingKtFile.aliasImportMap()[referencedName].singleOrNull() ?: referencedName
    }

    private fun retrieveJavaPlaceTargetClassesFQNs(place: InjectionPlace): Collection<String> {
        val classCondition = place.elementPattern.condition.conditions.firstOrNull { it.debugMethodName == "definedInClass" }
                as? PatternConditionPlus<*, *> ?: return emptyList()
        val psiClassNamePatternCondition =
            classCondition.valuePattern.condition.conditions.firstIsInstanceOrNull<PsiClassNamePatternCondition>() ?: return emptyList()
        val valuePatternCondition =
            psiClassNamePatternCondition.namePattern.condition.conditions.firstIsInstanceOrNull<ValuePatternCondition<String>>()
                ?: return emptyList()
        return valuePatternCondition.values
    }

    private fun retrieveKotlinPlaceTargetClassesFQNs(place: InjectionPlace): Collection<String> {
        val classNames = SmartList<String>()
        fun collect(condition: PatternCondition<*>) {
            when (condition) {
                is PatternConditionPlus<*, *> -> condition.valuePattern.condition.conditions.forEach { collect(it) }
                is KotlinFunctionPattern.DefinedInClassCondition -> classNames.add(condition.fqName)
            }
        }
        place.elementPattern.condition.conditions.forEach { collect(it) }
        return classNames
    }

}