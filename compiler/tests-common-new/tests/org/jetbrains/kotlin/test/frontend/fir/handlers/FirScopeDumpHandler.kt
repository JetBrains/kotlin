/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumperImpl
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent

class FirScopeDumpHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    private val dumper = MultiModuleInfoDumperImpl()

    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val fqNames = module.directives[FirDiagnosticsDirectives.SCOPE_DUMP]
        if (fqNames.isEmpty()) return
        val printer = SmartPrinter(dumper.builderForModule(module), indent = "  ")
        for (fqName in fqNames) {
            printer.processClass(fqName, info.session, info.firAnalyzerFacade.scopeSession, module)
        }
    }

    private fun SmartPrinter.processClass(fqName: String, session: FirSession, scopeSession: ScopeSession, module: TestModule) {
        val classId = ClassId.topLevel(FqName.fromSegments(fqName.split(".")))
        val symbol = session.symbolProvider.getClassLikeSymbolByFqName(classId) ?: assertions.fail {
            "Class $fqName not found in module ${module.name}"
        }
        val firClass = symbol.fir as? FirRegularClass ?: assertions.fail { "$fqName is not a class but ${symbol.fir.render()}" }
        println("$fqName: ")

        val scope = firClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
        val names = scope.getCallableNames()
        withIndent {
            for (name in names) {
                processFunctions(name, scope)
                processProperties(name, scope)
            }
        }
        println()
    }

    private class SymbolCounter {
        private val map = mutableMapOf<FirBasedSymbol<*>, Int>()
        private var counter = 0

        fun getIndex(symbol: FirBasedSymbol<*>): Int {
            return map.computeIfAbsent(symbol) { counter++ }
        }
    }

    private fun SmartPrinter.processFunctions(name: Name, scope: FirTypeScope) {
        val functions = scope.getFunctions(name)
        for (function in functions) {
            processFunction(function, scope, SymbolCounter())
            println()
        }
    }

    private fun SmartPrinter.processFunction(symbol: FirNamedFunctionSymbol, scope: FirTypeScope, counter: SymbolCounter) {
        printInfo(symbol.fir, counter)
        scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { overriden, baseScope ->
            withIndent {
                processFunction(overriden, baseScope, counter)
            }
            ProcessorAction.NEXT
        }
    }

    private fun SmartPrinter.processProperties(name: Name, scope: FirTypeScope) {
        val properties = scope.getProperties(name)
        for (property in properties) {
            if (property !is FirPropertySymbol) continue
            processProperty(property, scope, SymbolCounter())
            println()
        }
    }

    private fun SmartPrinter.processProperty(symbol: FirPropertySymbol, scope: FirTypeScope, counter: SymbolCounter) {
        printInfo(symbol.fir, counter)
        withIndent {
            scope.processDirectOverriddenPropertiesWithBaseScope(symbol) { overriden, baseScope ->
                processProperty(overriden, baseScope, counter)
                ProcessorAction.NEXT
            }
        }
    }

    private fun SmartPrinter.printInfo(declaration: FirCallableDeclaration, counter: SymbolCounter) {
        print("[${declaration.origin}]: ")
        print(declaration.render(FirRenderer.RenderMode.NoBodies).trim())
        print(" from ${declaration.dispatchReceiverType?.render()}")
        println(" [id: ${counter.getIndex(declaration.symbol)}]")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension(".overrides.txt")
        val actualDump = dumper.generateResultingDump()
        assertions.assertEqualsToFile(expectedFile, actualDump)
    }
}
