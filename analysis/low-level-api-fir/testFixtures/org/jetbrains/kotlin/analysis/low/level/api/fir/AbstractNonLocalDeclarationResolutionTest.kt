/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Resolves every non-local declaration of the [main file][doTestByMainFile] to FIR via [resolveToFirSymbol] and renders the result.
 *
 * The declarations are collected via the stub-based PSI API ([KtFile.getDeclarations]/[KtClassOrObject.getDeclarations]), and only
 * the resulting FIR structure is rendered, so neither the collection nor the rendering touches the file AST. Therefore, the test can
 * be run against a stub-based file via the
 * [STUB_BASED][org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives.STUB_BASED] directive to verify that finding
 * non-local declarations (via
 * [findSourceNonLocalFirDeclaration][org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration],
 * which builds the raw FIR of the whole file) does not require loading the file AST.
 *
 * Declarations are resolved only to [FirResolvePhase.RAW_FIR][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.RAW_FIR] on
 * purpose: resolving declaration bodies (e.g., a destructuring initializer) legitimately needs the AST and is out of scope here.
 */
abstract class AbstractNonLocalDeclarationResolutionTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val declarations = buildList {
            fun collect(declaration: KtDeclaration) {
                add(declaration)
                if (declaration is KtClassOrObject) {
                    declaration.declarations.forEach(::collect)
                }
            }

            mainFile.declarations.forEach(::collect)
        }

        val actual = withResolutionFacade(mainModule.ktModule) { resolutionFacade ->
            prettyPrint {
                for (declaration in declarations) {
                    val symbol = declaration.resolveToFirSymbol(resolutionFacade)
                    val name = (declaration as? KtNamedDeclaration)?.name?.let { " '$it'" } ?: ""
                    // The containing class is resolved via the stub-based PSI parent chain (no AST), so the labels of otherwise
                    // identically rendered declarations (such as destructuring declarations) stay distinguishable.
                    val container = declaration.containingClassOrObject?.name?.let { " in '$it'" } ?: " (top-level)"
                    appendLine("${declaration::class.simpleName}$name$container:")
                    withIndent {
                        appendLine(lazyResolveRenderer(StringBuilder()).renderElementAsString(symbol.fir, trim = true))
                    }

                    appendLine()
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

abstract class AbstractSourceLikeNonLocalDeclarationResolutionTest : AbstractNonLocalDeclarationResolutionTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}
