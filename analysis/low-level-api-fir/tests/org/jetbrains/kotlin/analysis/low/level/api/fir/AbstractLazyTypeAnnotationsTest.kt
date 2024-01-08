/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.project.structure.allKtFiles
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test exists to:
 * * Check how annotation propagation works (who is an owner of an annotation)
 * * Cover scenarios with lazy resolution from type position (this is a valid case of usage in Analysis API (KtFirAnnotationListForType))
 *
 * It collects all type annotations from the selected declaration and resolves them
 */
abstract class AbstractLazyTypeAnnotationsTest : AbstractFirLazyDeclarationResolveTestCase() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val builderBeforeAnnotationResolve = StringBuilder()
        val builderAfterAnnotationResolve = StringBuilder()

        val allKtFiles = testServices.allKtFiles()
        resolveWithClearCaches(mainFile) { session ->
            val moduleStructure = testServices.moduleStructure
            val (declaration, resolver) = findFirDeclarationToResolve(mainFile, moduleStructure, testServices, session)
            resolver.invoke(FirResolvePhase.TYPES)

            if (declaration is FirCallableDeclaration) {
                declaration.symbol.calculateReturnType()
            }

            if (Directives.BODY_RESOLVE in moduleStructure.allDirectives) {
                resolver.invoke(FirResolvePhase.BODY_RESOLVE)
            }

            val typesWithContext = declaration.collectConeTypes()
            val firFiles = allKtFiles.map(session::getOrBuildFirFile)
            dumpFir(typesWithContext, declaration, firFiles, builderBeforeAnnotationResolve)

            typesWithContext.forEach { typeWithContext ->
                typeWithContext.type.annotationContainingSymbols().forEach {
                    it.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
                }
            }

            dumpFir(typesWithContext, declaration, firFiles, builderAfterAnnotationResolve)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            builderBeforeAnnotationResolve.toString(),
            extension = "before.txt",
        )

        testServices.assertions.assertEqualsToTestDataFileSibling(
            builderAfterAnnotationResolve.toString(),
            extension = "after.txt",
        )
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    private object Directives : SimpleDirectivesContainer() {
        val BODY_RESOLVE by directive("Resolve a declaration to body to collect types from the body as well")
    }
}

private fun ConeKotlinType.annotationContainingSymbols(): List<FirBasedSymbol<*>> = customAnnotations.mapNotNull {
    (it as? FirAnnotationCall)?.containingDeclarationSymbol
}

abstract class AbstractSourceLazyTypeAnnotationsTest : AbstractLazyTypeAnnotationsTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptLazyTypeAnnotationsTest : AbstractLazyTypeAnnotationsTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootLazyTypeAnnotationsTest : AbstractLazyTypeAnnotationsTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

private fun dumpFir(
    typesWithContext: Collection<ConeTypeWithContext>,
    elementToDump: FirElementWithResolveState,
    filesToDump: List<FirFile>,
    builder: StringBuilder,
) {
    typesWithContext.forEach { typeWithContext ->
        builder.appendLine(typeWithContext.type)
        builder.append("  context -> ")
        builder.appendLine(typeWithContext.context)
        builder.append("  anchor -> ")
        builder.appendLine(typeWithContext.type.annotationContainingSymbols().map { it.toStringWithContext() })
        builder.appendLine()
    }

    val renderer = lazyResolveRenderer(builder)
    if (elementToDump !in filesToDump) {
        builder.append("\nTARGET: ")
        renderer.renderElementAsString(elementToDump)
    }

    for (file in filesToDump) {
        renderer.renderElementAsString(file)
    }
}

private fun FirBasedSymbol<*>.toStringWithContext(): String {
    val base = toString()
    val parentSymbol: FirBasedSymbol<*>? = when (this) {
        is FirValueParameterSymbol -> containingFunctionSymbol
        is FirPropertyAccessorSymbol -> propertySymbol
        is FirTypeParameterSymbol -> containingDeclarationSymbol
        is FirBackingFieldSymbol -> propertySymbol
        else -> null
    }

    return base + parentSymbol?.let { " from ${it.toStringWithContext()}" }.orEmpty()
}

private class ConeTypeWithContext(val type: ConeKotlinType, val context: String)

private fun FirElementWithResolveState.collectConeTypes(): Collection<ConeTypeWithContext> {
    val types = mutableListOf<ConeTypeWithContext>()
    val contextStack = ContextStack()

    this.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            contextStack.withStack(element) {
                when (element) {
                    is FirResolvedTypeRef -> element.type.forEachType {
                        if (it.customAnnotations.isNotEmpty()) {
                            types += ConeTypeWithContext(it, contextStack.dumpContext())
                        }
                    }
                    is FirLazyBlock -> return
                }

                // otherwise, we will visit only the getter return type and status
                // we will use acceptChildren anyway
                // to be sure that we are not missed any potential changes in FirSyntheticProperty
                if (element is FirSyntheticProperty) {
                    element.getter.accept(this)
                    element.setter?.accept(this)
                }

                element.acceptChildren(this)
            }
        }
    })

    return types
}

private class ContextStack {
    val stack = mutableListOf<FirDeclaration>()

    inline fun withStack(element: FirElement, action: () -> Unit) {
        if (element is FirDeclaration) {
            stack += element
        }

        try {
            action()
        } finally {
            if (element is FirDeclaration) {
                val last = stack.removeLast()
                if (last != element) {
                    error("Stack is corrupted")
                }
            }
        }
    }

    fun dumpContext(): String = stack.last().symbol.toStringWithContext()
}
