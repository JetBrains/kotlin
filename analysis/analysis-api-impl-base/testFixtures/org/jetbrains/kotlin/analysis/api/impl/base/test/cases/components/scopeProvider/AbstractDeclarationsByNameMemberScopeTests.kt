/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.memberScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Shared base for tests of [KaScope.declarations] overloads with a name filter / name collection.
 *
 * The set of names to include is taken from the `NAME` directive (one identifier per occurrence).
 */
@OptIn(KaExperimentalApi::class)
abstract class AbstractDeclarationsByNameMemberScopeTestBase : AbstractMemberScopeTestBase() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    protected lateinit var requestedNames: Set<Name>
        private set

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        requestedNames = testServices.moduleStructure.allDirectives[Directives.NAME].toSet()
        super.doTestByMainFile(mainFile, mainModule, testServices)
    }

    context(_: KaSession)
    override fun getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.memberScope

    protected object Directives : SimpleDirectivesContainer() {
        val NAME by valueDirective(
            description = "A declaration name to include in the filter. May be specified multiple times.",
            parser = Name::identifier,
        )
    }
}

/**
 * Tests `KaScope.declarations(nameFilter)` on a class member scope, filtering by the `NAME` directive.
 */
@OptIn(KaExperimentalApi::class)
abstract class AbstractDeclarationsByNameFilterMemberScopeTest : AbstractDeclarationsByNameMemberScopeTestBase() {
    context(_: KaSession)
    override fun getSymbolsFromScope(scope: KaScope): Sequence<KaDeclarationSymbol> =
        scope.declarations { it in requestedNames }
}

/**
 * Tests `KaScope.declarations(names)` on a class member scope, passing the names from the `NAME` directive.
 */
@OptIn(KaExperimentalApi::class)
abstract class AbstractDeclarationsByNamesMemberScopeTest : AbstractDeclarationsByNameMemberScopeTestBase() {
    context(_: KaSession)
    override fun getSymbolsFromScope(scope: KaScope): Sequence<KaDeclarationSymbol> =
        scope.declarations(requestedNames)
}
