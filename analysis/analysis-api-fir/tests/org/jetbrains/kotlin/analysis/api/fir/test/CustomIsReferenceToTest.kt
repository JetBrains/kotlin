/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUserType
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomIsReferenceToTest : AbstractAnalysisApiExecutionTest("../analysis-api/testData/references/customIsReferenceTo") {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun defaultNoArgConstructor(mainFile: KtFile) {
        val fooClass = mainFile.declarations.first() as KtClass
        val lightClass = fooClass.toLightClass() ?: error("Light class cannot be created")
        val lightConstructor = lightClass.constructors.single() as KtLightMethod

        val usageFunction = mainFile.declarations.last() as KtNamedFunction
        val typeElement = usageFunction.valueParameters.single().typeReference?.typeElement as KtUserType
        val typeReference = typeElement.referenceExpression?.mainReference
        assertNotNull(typeReference)

        assertTrue(typeReference.isReferenceTo(lightClass))
        assertFalse(typeReference.isReferenceTo(lightConstructor))

        val constructorCall = usageFunction.bodyBlockExpression?.statements?.single() as KtCallExpression
        val constructorReference = constructorCall.calleeExpression?.mainReference
        assertNotNull(constructorReference)

        assertTrue(constructorReference.isReferenceTo(lightClass))
        assertTrue(constructorReference.isReferenceTo(lightConstructor))
    }
}