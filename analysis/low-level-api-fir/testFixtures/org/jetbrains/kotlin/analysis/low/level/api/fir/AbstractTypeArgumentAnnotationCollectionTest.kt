/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLSubstitutionScopeKeyFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.scopes.substitutionScopeKeyFactory
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.test.fail

/**
 * Tests the annotation collection logic used by [LLSubstitutionScopeKeyFactory] to prevent scope cache corruption when the same nominal
 * type appears with different type argument annotations.
 */
abstract class AbstractTypeArgumentAnnotationCollectionTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val typeReference = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfType<KtTypeReference>(mainFile)

        withResolutionFacade(mainModule.ktModule) { resolutionFacade ->
            val coneType = when (val firElement = typeReference.getOrBuildFir(resolutionFacade)) {
                is FirResolvedTypeRef -> firElement.coneType
                else -> fail("Unresolved type reference: ${typeReference.text}")
            }

            require(coneType is ConeClassLikeType)
            val coneArgumentTypes = coneType.typeArguments.map { it.type }

            val substitutionScopeFactory = resolutionFacade.useSiteFirSession.substitutionScopeKeyFactory
            require(substitutionScopeFactory is LLSubstitutionScopeKeyFactory)

            val annotations = substitutionScopeFactory.collectPlacedAnnotations(coneArgumentTypes)

            val actualText = buildString {
                FirRenderer(this).typeRenderer.render(coneType)
                appendLine().appendLine()

                if (annotations.isNotEmpty()) {
                    annotations.joinTo(this, separator = "\n") { it.toString() }
                } else {
                    append("No annotations")
                }
            }

            testServices.assertions.assertEqualsToTestOutputFile(actualText)
        }
    }
}

abstract class AbstractSourceTypeArgumentAnnotationCollectionTest : AbstractTypeArgumentAnnotationCollectionTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}
