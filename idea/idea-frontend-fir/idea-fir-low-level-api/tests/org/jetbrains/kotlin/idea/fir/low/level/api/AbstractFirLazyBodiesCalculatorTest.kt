/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractFirLazyBodiesCalculatorTest : AbstractLowLevelApiSingleFileTest() {

    private val lazyChecker = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            TestCase.assertFalse("${FirLazyBlock::class.qualifiedName} should not present in the tree", element is FirLazyBlock)
            TestCase.assertFalse("${FirLazyExpression::class.qualifiedName} should not present in the tree", element is FirLazyExpression)
            element.acceptChildren(this)
        }
    }

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        resolveWithClearCaches(ktFile) { resolveState ->
            val session = resolveState.rootModuleSession
            val provider = session.firIdeProvider.kotlinScopeProvider

            val laziedFirFile = RawFirBuilder(
                session,
                provider,
                psiMode = PsiHandlingMode.IDE,
                bodyBuildingMode = BodyBuildingMode.LAZY_BODIES
            ).buildFirFile(ktFile)

            FirLazyBodiesCalculator.calculateLazyBodies(laziedFirFile)
            laziedFirFile.accept(lazyChecker)

            val fullFirFile = RawFirBuilder(
                session,
                provider,
                psiMode = PsiHandlingMode.IDE,
                bodyBuildingMode = BodyBuildingMode.NORMAL
            ).buildFirFile(ktFile)

            val laziedFirFileDump = StringBuilder().also { FirRenderer(it).visitFile(laziedFirFile) }.toString()
            val fullFirFileDump = StringBuilder().also { FirRenderer(it).visitFile(fullFirFile) }.toString()

            TestCase.assertEquals(laziedFirFileDump, fullFirFileDump)
        }
    }

}