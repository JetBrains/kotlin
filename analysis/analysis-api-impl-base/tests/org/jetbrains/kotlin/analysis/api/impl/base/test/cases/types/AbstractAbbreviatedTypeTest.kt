/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.SymbolByFqName
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Renders the properties and the textual representation of a callable's return type specified via `// callable: ID`. The varying
 * [TestModuleKind][org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind] supplied by generated tests applies to
 * the expansion-site module instead of the use-site module in these tests, as we want to check the `KaType` when it's built with an
 * expanded type from a library vs. from a source module. (Abbreviated types from libraries and from sources aren't necessarily constructed
 * in the same way.)
 *
 * This test is different from [AbstractTypeByDeclarationReturnTypeTest] because we want to generate tests for extension-site modules with
 * [TestModuleKind.Source][org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind.Source] and
 * [TestModuleKind.LibraryBinary][org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind.LibraryBinary]. It wouldn't
 * be appropriate to generate [AbstractTypeByDeclarationReturnTypeTest] for `LibraryBinary`.
 */
abstract class AbstractAbbreviatedTypeTest : AbstractTypeTest() {
    override fun getType(analysisSession: KaSession, ktFile: KtFile, module: KtTestModule, testServices: TestServices): KaType {
        val callableSymbol = with(SymbolByFqName.getSymbolDataFromFile(testDataPath)) {
            with(analysisSession) {
                toSymbols(ktFile).singleOrNull() as? KaCallableSymbol ?: error("Expected a single callable declaration to be specified.")
            }
        }
        return callableSymbol.returnType
    }
}
