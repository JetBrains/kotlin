/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hiddenEverywhereBesideSuperCallsStatus
import org.jetbrains.kotlin.fir.declarations.isHiddenToOvercomeSignatureClash
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

@OptIn(SymbolInternals::class)
class FirScopeDumpHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    private val dumper = MultiModuleInfoDumper()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            val fqNamesWithNames = currentModule.directives[FirDiagnosticsDirectives.SCOPE_DUMP]
            if (fqNamesWithNames.isEmpty()) return
            val printer = SmartPrinter(dumper.builderForModule(currentModule), indent = "  ")
            for (fqNameWithNames in fqNamesWithNames) {
                val (fqName, names) = extractFqNameAndMemberNames(fqNameWithNames)
                printer.processClass(fqName, names, part.session, part.firAnalyzerFacade.scopeSession, currentModule)
            }
        }
    }

    private fun extractFqNameAndMemberNames(fqNameWithNames: String): Pair<String, List<String>> {
        val (fqName, namesString) = fqNameWithNames.split(":").takeIf { it.size > 1 } ?: return fqNameWithNames to emptyList()
        return fqName to namesString.split(";")
    }

    private fun SmartPrinter.processClass(
        fqName: String,
        namesFromDirective: List<String>,
        session: FirSession,
        scopeSession: ScopeSession,
        module: TestModule
    ) {
        val (packageFqName, className) = fqName.split(".").let {
            val packageName = FqName.fromSegments(it.dropLast(1))
            packageName to it.last()
        }
        val classId = className.let {
            val names = it.split("$")
            var classId = ClassId(packageFqName, Name.identifier(names.first()))
            for (name in names.drop(1)) {
                classId = classId.createNestedClassId(Name.identifier(name))
            }
            classId
        }
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: assertions.fail {
            "Class $fqName not found in module ${module.name}"
        }
        val firClass = symbol.fir as? FirRegularClass ?: assertions.fail { "$fqName is not a class but ${symbol.fir.render()}" }
        println("$fqName: ")

        session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            val scope = firClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
            val names = namesFromDirective.takeIf { it.isNotEmpty() }?.map { Name.identifier(it) } ?: scope.getCallableNames()
            withIndent {
                for (name in names) {
                    processFunctions(name, scope)
                    processProperties(name, scope)
                }
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
        }
    }

    private fun SmartPrinter.processFunction(symbol: FirNamedFunctionSymbol, scope: FirTypeScope, counter: SymbolCounter) {
        printInfo(symbol.fir, scope, counter)
        scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { overridden, baseScope ->
            withIndent {
                processFunction(overridden, baseScope, counter)
            }
            ProcessorAction.NEXT
        }
    }

    private fun SmartPrinter.processProperties(name: Name, scope: FirTypeScope) {
        val properties = scope.getProperties(name)
        for (property in properties) {
            processProperty(property, scope, SymbolCounter())
        }
    }

    private fun SmartPrinter.processProperty(symbol: FirVariableSymbol<*>, scope: FirTypeScope, counter: SymbolCounter) {
        printInfo(symbol.fir, scope, counter)
        if (symbol !is FirPropertySymbol) return
        withIndent {
            scope.processDirectOverriddenPropertiesWithBaseScope(symbol) { overriden, baseScope ->
                processProperty(overriden, baseScope, counter)
                ProcessorAction.NEXT
            }
        }
    }

    private fun SmartPrinter.printInfo(declaration: FirCallableDeclaration, scope: FirTypeScope, counter: SymbolCounter) {
        val origin = declaration.origin.takeUnless { it == FirDeclarationOrigin.BuiltIns } ?: FirDeclarationOrigin.Library
        print("[$origin]: ")
        if (declaration.hiddenEverywhereBesideSuperCallsStatus != null) {
            print("/* hidden beside supers */ ")
        } else if (declaration.isHiddenToOvercomeSignatureClash == true) {
            print("/* hidden due to clash */ ")
        }
        val renderedDeclaration = FirRenderer.noAnnotationBodiesAccessorAndArguments().renderElementAsString(declaration).trim()
        print(renderedDeclaration)
        print(" from $scope")
        println(" [id: ${counter.getIndex(declaration.symbol)}]")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension(".overrides.txt")
        val actualDump = dumper.generateResultingDump()
        assertions.assertEqualsToFile(expectedFile, actualDump)
    }
}
