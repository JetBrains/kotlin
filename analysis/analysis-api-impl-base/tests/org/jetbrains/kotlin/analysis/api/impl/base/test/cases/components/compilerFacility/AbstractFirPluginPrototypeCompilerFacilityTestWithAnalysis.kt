/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.test.services.TestServices

/**
 * The IDE can run analysis before calling CodeGen API. We found some cases that generate different FIR expressions between analysis
 * and CodeGen API. For example, AA generates `FirLiteralExpression` for an initializer of a property, while CodeGen API generates
 * `FirLazyExpression`. The difference can result in crashes. This class helps us test the IDE situation that first calls analysis
 * APIs and conducts CodeGen.
 */
abstract class AbstractFirPluginPrototypeCompilerFacilityTestWithAnalysis : AbstractFirPluginPrototypeMultiModuleCompilerFacilityTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        // First, run AA for type resolution.
        testServices.ktTestModuleStructure.allMainKtFiles.forEach { file ->
            file.accept(object : KtTreeVisitorVoid() {
                override fun visitDestructuringDeclaration(declaration: KtDestructuringDeclaration) {
                    analyze(declaration) {
                        val initializer = declaration.initializer ?: return@analyze
                        initializer.getKtType()
                    }
                }
            })
        }
        // After running AA, run CodeGen.
        super.doTestByMainFile(mainFile, mainModule, testServices)
    }
}