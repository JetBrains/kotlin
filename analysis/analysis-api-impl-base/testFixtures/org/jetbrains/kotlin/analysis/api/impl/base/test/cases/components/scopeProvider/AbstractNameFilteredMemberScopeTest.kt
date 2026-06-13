/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.render
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Exercises the name-filtered [KaScope] APIs on a class member scope in a single test:
 *
 * - [KaScope.declarations] — `(Name) -> Boolean`, `Collection<Name>`, `vararg Name`
 * - [KaScope.callables]    — `(Name) -> Boolean`, `Collection<Name>`, `vararg Name`
 * - [KaScope.classifiers]  — `(Name) -> Boolean`, `Collection<Name>`, `vararg Name`
 *
 * The set of names is taken from the `NAME` directive (one identifier per occurrence).
 *
 * Within each kind (`declarations` / `callables` / `classifiers`), all three overloads are required to yield the same *set* of
 * symbols — they only differ in implementation strategy, not contract. The test asserts this set-equality at runtime and
 * renders just one section per kind into `.pretty.txt`, so ordering differences between the overloads (which are
 * implementation details) don't leak into test data.
 *
 * Two output files are produced per test case:
 * - `.names.txt` — scope-level possible names (independent of the filter).
 * - `.pretty.txt` — one section per kind, showing the symbols returned by the `nameFilter` overload (source order).
 */
abstract class AbstractNameFilteredMemberScopeTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private val prettyRenderer: KaDeclarationRenderer get() = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES

    private object Directives : SimpleDirectivesContainer() {
        val NAME by valueDirective(
            description = "A declaration name to include in the filter. May be specified multiple times.",
            parser = Name::identifier,
        )
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val requestedNames = testServices.moduleStructure.allDirectives[Directives.NAME].toSet()

        analyzeForTest(mainFile) {
            val container = getSingleTestTargetSymbolOfType<KaDeclarationContainerSymbol>(testDataPath, mainFile)
            val scope = container.memberScope

            val actualNames = prettyPrint { renderNamesContainedInScope(scope) }
            testServices.assertions.assertEqualsToTestOutputFile(actualNames, extension = ".names.txt")

            val nameFilter: (Name) -> Boolean = { it in requestedNames }
            val varargNames = requestedNames.toTypedArray()
            val combined = prettyPrint {
                renderAndAssertSameSet(
                    apiName = "declarations",
                    fromNameFilter = scope.declarations(nameFilter),
                    fromCollection = scope.declarations(requestedNames),
                    fromVararg = scope.declarations(*varargNames),
                    testServices = testServices,
                )
                renderAndAssertSameSet(
                    apiName = "callables",
                    fromNameFilter = scope.callables(nameFilter),
                    fromCollection = scope.callables(requestedNames),
                    fromVararg = scope.callables(*varargNames),
                    testServices = testServices,
                )
                renderAndAssertSameSet(
                    apiName = "classifiers",
                    fromNameFilter = scope.classifiers(nameFilter),
                    fromCollection = scope.classifiers(requestedNames),
                    fromVararg = scope.classifiers(*varargNames),
                    testServices = testServices,
                )
            }
            testServices.assertions.assertEqualsToTestOutputFile(combined, extension = ".pretty.txt")
        }
    }

    context(_: KaSession)
    private fun PrettyPrinter.renderAndAssertSameSet(
        apiName: String,
        fromNameFilter: Sequence<KaDeclarationSymbol>,
        fromCollection: Sequence<KaDeclarationSymbol>,
        fromVararg: Sequence<KaDeclarationSymbol>,
        testServices: TestServices,
    ) {
        val byNameFilter: List<String> = fromNameFilter.map { it.render(prettyRenderer) }.toList() // list for stable ordering in test data
        val byCollection: Set<String> = fromCollection.mapTo(mutableSetOf()) { it.render(prettyRenderer) }
        val byVararg: Set<String> = fromVararg.mapTo(mutableSetOf()) { it.render(prettyRenderer) }

        testServices.assertions.assertEquals(byNameFilter.toSet(), byCollection) {
            "`$apiName(nameFilter)` and `$apiName(names: Collection<Name>)` produce different sets"
        }
        testServices.assertions.assertEquals(byCollection, byVararg) {
            "`$apiName(names: Collection<Name>)` and `$apiName(vararg names)` produce different sets"
        }

        appendLine("$apiName:")
        withIndent {
            if (byNameFilter.isEmpty()) {
                appendLine("NO_SYMBOLS")
            } else {
                byNameFilter.forEach(::appendLine)
            }
        }
        appendLine()
    }
}
