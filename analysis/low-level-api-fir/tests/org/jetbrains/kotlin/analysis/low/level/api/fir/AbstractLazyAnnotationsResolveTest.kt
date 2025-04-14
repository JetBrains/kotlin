/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.renderer.FirErrorExpressionExtendedRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This class is supposed to test two things:
 * - The correctness of [KaAnnotationList] API
 * - Lazy resolution contracts for [KaAnnotationList] API and how much resolution is required
 */
abstract class AbstractLazyAnnotationsResolveTest : AbstractFirLazyDeclarationResolveTestCase() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        withResolutionFacade(mainFile) { resolutionFacade ->
            val (firElement, _) = findFirDeclarationToResolve(mainFile, testServices, resolutionFacade)
            val psiElement = firElement.realPsi as? KtAnnotated

            // Dump FIR before any potential resolution
            testServices.assertions.assertFirDump(firElement, phase = "before")

            analyseForTest(mainFile) {
                val symbol = when {
                    psiElement == null && firElement is FirBackingField -> {
                        val ktProperty = firElement.psi as KtProperty
                        val propertySymbol = ktProperty.symbol as KaPropertySymbol
                        propertySymbol.backingFieldSymbol!!
                    }

                    psiElement is KtFile -> psiElement.symbol
                    psiElement is KtDeclaration -> psiElement.symbol

                    else -> getSingleTestTargetSymbolOfType<KaAnnotatedSymbol>(testDataPath, mainFile)
                }

                val query = testServices.moduleStructure.allDirectives.singleValue(Directives.QUERY)
                with(AnnotationQuery.create(query)) {
                    val output = performAndRender(symbol) {
                        // Dump FIR right away after one specific operation
                        testServices.assertions.assertFirDump(firElement, phase = "intermediate")
                    }

                    testServices.assertions.assertEqualsToTestOutputFile(output, extension = ".out.txt")
                }

                // Dump FIR after output render as it also may trigger additional resolution
                testServices.assertions.assertFirDump(firElement, phase = "after")
            }
        }
    }

    /**
     * Dumps basic information about [element] to avoid unnecessary noise in test data.
     *
     * More rich output can be achieved by other [AbstractFirLazyDeclarationResolveTestCase] implementations.
     */
    private fun AssertionsService.assertFirDump(element: FirElementWithResolveState, phase: String) {
        val renderer = FirRenderer(
            builder = StringBuilder(),
            bodyRenderer = null,
            classMemberRenderer = null,
            contractRenderer = null,
            resolvePhaseRenderer = FirResolvePhaseRenderer(),
            errorExpressionRenderer = FirErrorExpressionExtendedRenderer(),
        )

        val firDumpAfter = renderer.renderElementAsString(element)
        assertEqualsToTestOutputFile(firDumpAfter, extension = "fir.$phase.txt")
    }

    private object Directives : SimpleDirectivesContainer() {
        val QUERY: StringDirective by stringDirective("Annotation query to perform", multiLine = true)
    }
}

abstract class AbstractSourceLazyAnnotationsResolveTest : AbstractLazyAnnotationsResolveTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptLazyAnnotationsResolveTest : AbstractLazyAnnotationsResolveTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}

/**
 * This class represents a query to [KaAnnotatedSymbol]. It exists for two purposes:
 *
 * - Check the resolution state to explicitly declare how much resolution is required
 * to perform a particular operation.
 * - Check the output of the method which the class describes.
 *
 * [perform] method is narrowly targeted query for a symbol.
 *
 * [renderOutput] method represents the output from [perform] method.
 *
 * @see perform
 * @see renderOutput
 */
@OptIn(KaNonPublicApi::class)
private sealed class AnnotationQuery<O : Any> {
    fun KaSession.performAndRender(symbol: KaAnnotatedSymbol, intermediateStep: () -> Unit): String {
        val output = perform(symbol)
        intermediateStep()
        return renderOutput(output)
    }

    abstract fun KaSession.perform(symbol: KaAnnotatedSymbol): O

    /**
     * Note: this method may trigger additional resolution, so it should be called
     * only after the resolution state check.
     */
    abstract fun KaSession.renderOutput(output: O): String

    /**
     * Represents [KaAnnotationList.contains]
     */
    class Contains(val classId: ClassId) : AnnotationQuery<Boolean>() {
        override fun KaSession.perform(symbol: KaAnnotatedSymbol): Boolean = classId in symbol.annotations
        override fun KaSession.renderOutput(output: Boolean): String = output.toString()
    }

    sealed class CollectionQuery<O : Any> : AnnotationQuery<Collection<O>>() {
        final override fun KaSession.renderOutput(output: Collection<O>): String = prettyPrint {
            if (output.isEmpty()) {
                append("[]")
                return@prettyPrint
            }

            printCollection(output, separator = "\n\n") { element ->
                append(renderElementOutput(element))
            }
        }

        abstract fun KaSession.renderElementOutput(element: O): String

        sealed class AnnotationQuery : CollectionQuery<KaAnnotation>() {
            final override fun KaSession.renderElementOutput(element: KaAnnotation): String {
                return DebugSymbolRenderer().renderAnnotationApplication(this, element)
            }

            /**
             * Represents [KaAnnotationList]
             */
            object Annotations : AnnotationQuery() {
                override fun KaSession.perform(symbol: KaAnnotatedSymbol): List<KaAnnotation> = symbol.annotations
            }

            /**
             * Represents [KaAnnotationList.get]
             */
            class Get(val classId: ClassId) : AnnotationQuery() {
                override fun KaSession.perform(symbol: KaAnnotatedSymbol): List<KaAnnotation> = symbol.annotations[classId]
            }
        }

        /**
         * Represents [KaAnnotationList.classIds]
         */
        object ClassIds : CollectionQuery<ClassId>() {
            override fun KaSession.perform(symbol: KaAnnotatedSymbol): Collection<ClassId> = symbol.annotations.classIds
            override fun KaSession.renderElementOutput(element: ClassId): String = element.toString()
        }
    }

    companion object {
        fun create(query: String): AnnotationQuery<*> {
            val key = query.substringBefore(":")
            val value = query.substringAfter(":").trim()
            return when (key) {
                "contains" -> Contains(ClassId.fromString(value))
                "classIds" -> CollectionQuery.ClassIds
                "annotations" -> CollectionQuery.AnnotationQuery.Annotations
                "get" -> CollectionQuery.AnnotationQuery.Get(ClassId.fromString(value))
                else -> error("Unknown query: $query")
            }
        }
    }
}
