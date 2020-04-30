/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.codevision

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.AttributesTransformerPresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.ArrayUtil
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.search.declarationsSearch.toPossiblyFakeLightMethods
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinDefinitionsSearcher
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.utils.SmartList


@Suppress("UnstableApiUsage")
class KotlinCodeVisionHintsCollector(
    editor: Editor, private val showUsages: Boolean, private val showInheritors: Boolean,
    private val usagesLimit: Int, private val inheritorsLimit: Int
) : FactoryInlayHintsCollector(editor) {

    companion object {
        private val hintsKey = Key.create<CachedValue<CachedHints>>("CodeVisionHints")


        fun searchUsages(element: PsiElement, usagesLimit: Int): Usages? {
            val countingProcessor = CountingUpToLimitProcessor<Any>(usagesLimit)
            if (element is KtClass) {
                ReferencesSearch.search(element).forEach(countingProcessor)
            } else {
                element.toPossiblyFakeLightMethods().firstOrNull() // inline methods don't have light analogues (fake needed)
                    ?.let { MethodReferencesSearch.search(it).forEach(countingProcessor) }
            }

            val (usagesNum, limitReached) = countingProcessor
            return if (usagesNum > 0) Usages(usagesNum, limitReached) else null
        }

        private fun searchFunctionOverrides(function: KtFunction, inheritorsLimit: Int): KotlinCodeVisionLimitedHint? {
            return LightClassUtil.getLightClassMethod(function)?.let { it ->
                val countingProcessor = CountingUpToLimitProcessor<Any>(inheritorsLimit)
                OverridingMethodsSearch.search(it, true).forEach(countingProcessor)
                val (overridingNum, limitReached) = countingProcessor

                if (overridingNum > 0) {
                    if (function.isAbstract()) FunctionImplementations(overridingNum, limitReached)
                    else FunctionOverrides(overridingNum, limitReached)
                } else null
            }
        }

        private fun searchClassInheritors(clazz: KtClass, inheritorsLimit: Int): KotlinCodeVisionLimitedHint? {
            return clazz.toLightClass()?.let {
                val countingProcessor = CountingUpToLimitProcessor<Any>(inheritorsLimit)
                DirectClassInheritorsSearch.search(it, clazz.useScope, true).forEach(countingProcessor)
                val (inheritorsNum, limitReached) = countingProcessor
                if (inheritorsNum > 0) {
                    if (clazz.isInterface()) InterfaceImplementations(inheritorsNum, limitReached)
                    else ClassInheritors(inheritorsNum, limitReached)
                } else null
            }
        }

        private fun searchPropertyOverriding(property: KtProperty, inheritorsLimit: Int): KotlinCodeVisionLimitedHint? {
            val countingProcessor = CountingUpToLimitProcessor<PsiElement>(inheritorsLimit)
            KotlinDefinitionsSearcher.processPropertyImplementationsMethods(
                property.toPossiblyFakeLightMethods(),
                GlobalSearchScope.allScope(property.project),
                countingProcessor
            )
            val (overridesNum, limitReached) = countingProcessor
            return if (overridesNum > 0) PropertyOverrides(overridesNum, limitReached) else null
        }
    }

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!showUsages && !showInheritors) return false
        if (!isElementOfInterest(element)) return true
        if (editor.project == null) return false

        val hints = retrieveHints(editor.project!!, element)
        val inlayPresentations =
            hints.takeIf { it.isNotEmpty() }?.let { listOf(prepareBlockElements(element, editor, hints)) } ?: emptyList()

        inlayPresentations.forEach { hintPair ->
            sink.addBlockElement(
                hintPair.first,
                relatesToPrecedingText = true,
                showAbove = true,
                priority = 0,
                presentation = hintPair.second
            )
        }

        return true
    }

    private fun retrieveHints(project: Project, element: PsiElement): List<KotlinCodeVisionLimitedHint> {
        val cachedValueProvider = CachedHintsProvider(
            element, showUsages, showInheritors, usagesLimit, inheritorsLimit
        )

        return CachedValuesManager.getManager(project).run {
            this.getCachedValue(element, hintsKey, cachedValueProvider, false).takeUnless { isStale(it) } ?: run {
                this.createCachedValue(cachedValueProvider).also { element.putUserData(hintsKey, it) }.value
            }
        }.hints
    }

    private fun isStale(cachedValue: CachedHints): Boolean {
        return cachedValue.showUsages != showUsages || cachedValue.showInheritors != showInheritors
    }

    @Suppress("GrazieInspection")
    private fun prepareBlockElements(
        element: PsiElement,
        editor: Editor,
        hints: List<KotlinCodeVisionHint>
    ): Pair<Int, InlayPresentation> {

        assert(hints.isNotEmpty()) { "Attempt to build block elements whereas hints don't exist" }
        assert(hints.size <= 2) { "Hints other than usages-implementations are not expected" }

        val offset = element.textRange.startOffset
        val line = editor.document.getLineNumber(offset)
        val lineStart = editor.document.getLineStartOffset(line)
        val indent = offset - lineStart

        /*
         * presentations: <indent>[<Usages>][<space><Inheritors>]
         * hints:                  hint[0]             hint[1]
         */
        val presentations = arrayOfNulls<InlayPresentation>(hints.size * 2) // 2 or 4
        presentations[0] = factory.text(StringUtil.repeat(" ", indent))
        var pInd = 1
        for (hInd in hints.indices) { // handling usages & inheritors
            val hint: KotlinCodeVisionHint = hints[hInd]
            if (hInd != 0)
                presentations[pInd++] = factory.text(" ")

            presentations[pInd++] = createPresentation(factory, element, editor, hint) // either Usages or Inheritors
        }

        val filledPresentations = presentations.requireNoNulls()

        val seq = factory.seq(*filledPresentations)
        val withAppearingSettings = factory.changeOnHover(seq, {
            val spaceAndSettings = arrayOf(factory.text(" "), createSettings(factory, element, editor))
            val withSettings = ArrayUtil.mergeArrays(filledPresentations, spaceAndSettings)
            factory.seq(*withSettings)
        }) { true }

        return Pair(lineStart, withAppearingSettings)
    }

    private fun isElementOfInterest(element: PsiElement): Boolean =
        element is KtClass
                || (element is KtFunction && element !is KtFunctionLiteral)
                || (element is KtProperty && !element.isLocal)

    private fun createPresentation(
        factory: PresentationFactory, element: PsiElement, editor: Editor, result: KotlinCodeVisionHint
    ): InlayPresentation {
        val text = factory.smallText(result.regularText)
        return factory.changeOnHover(text, {
            val onClick = factory.onClick(text, MouseButton.Left)
            { event, _ -> result.onClick(editor, element, event) }
            applyReferenceColor(onClick)
        }) { true }
    }

    private fun applyReferenceColor(presentation: InlayPresentation): InlayPresentation {
        return AttributesTransformerPresentation(presentation) {
            val attributes = EditorColorsManager.getInstance()
                .globalScheme.getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR).clone()
            attributes.apply { effectType = null }
        }
    }

    private fun createSettings(factory: PresentationFactory, element: PsiElement, editor: Editor): InlayPresentation {
        return createPresentation(factory, element, editor, SettingsHint())
    }

    class CachedHintsProvider(
        private val element: PsiElement,
        private val showUsages: Boolean,
        private val showInheritors: Boolean,
        private val usagesLimit: Int,
        private val inheritorsLimit: Int
    ) : CachedValueProvider<CachedHints> {

        override fun compute(): CachedValueProvider.Result<CachedHints>? {
            val hints: MutableList<KotlinCodeVisionLimitedHint> = SmartList()

            if (showUsages)
                searchUsages(element, usagesLimit)?.let { hints += it }

            if (showInheritors) {
                when (element) {
                    is KtFunction -> searchFunctionOverrides(element, inheritorsLimit)?.let { hints += it }
                    is KtClass -> searchClassInheritors(element, inheritorsLimit)?.let { hints += it }
                    is KtProperty -> searchPropertyOverriding(element, inheritorsLimit)?.let { hints += it }
                }
            }

            return CachedValueProvider.Result(
                CachedHints(hints, this.showUsages, this.showInheritors),
                // kotlinOutOfCodeBlockTracker doesn't work for usages: new reference inside the block is not detected
                // MODIFICATION_COUNT {+: file reopening -> cache hit, -: any code modification resets all caches (records are in PSI)}
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootModificationTracker.getInstance(element.project) // tracking project structure changes
            )
        }
    }

    class CountingUpToLimitProcessor<T>(private val limit: Int) : Processor<T> {
        private val findings = mutableSetOf<T>() // for properties it's crucial not to calculate setters and getters together

        override fun process(t: T): Boolean {
            findings.add(t)
            return findings.size < limit
        }

        operator fun component1(): Int = findings.size
        operator fun component2(): Boolean = findings.size >= limit
    }

    data class CachedHints(
        val hints: List<KotlinCodeVisionLimitedHint>,
        val showUsages: Boolean,
        val showInheritors: Boolean
    )
}