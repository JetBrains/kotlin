/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.KotlinClassifierStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

object SourceStubsTestEngine : StubsTestEngine() {
    override fun compute(file: KtFile): KotlinFileStubImpl = file.calcStubTree().root as KotlinFileStubImpl

    context(testContext: AbstractAnalysisApiBasedTest)
    override fun validate(testServices: TestServices, file: KtFile, fileStub: KotlinFileStubImpl) {
        super.validate(testServices, file, fileStub)

        val violations = mutableListOf<String>()
        checkPsiConsistencies(violations, fileStub)
        val violationsDump = violations.ifNotEmpty { joinToString("\n", postfix = "\n") }
        testContext.assertEqualsToTestOutputFile(
            violationsDump,
            extension = ".psiConsistencyViolations.txt",
        )
    }

    private fun checkPsiConsistencies(violations: MutableList<String>, stubElement: StubElement<*>) {
        val psi = stubElement.psi as? StubBasedPsiElement<*>
        if (psi is KtClassLikeDeclaration) {
            val stubClassId = (stubElement as KotlinClassifierStub).classId

            @Suppress("INVISIBLE_REFERENCE") // A workaround to not expose the calculator
            val psiClassId = org.jetbrains.kotlin.psi.psiUtil.ClassIdCalculator.calculateClassId(psi)
            if (psiClassId != stubClassId) {
                violations += "Class ID mismatch: PSI: $psiClassId, stub: $stubClassId"
            }
        }

        stubElement.childrenStubs.forEach {
            checkPsiConsistencies(violations, it)
        }
    }
}
