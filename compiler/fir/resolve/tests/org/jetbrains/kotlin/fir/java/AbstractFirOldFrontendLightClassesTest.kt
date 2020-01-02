/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.AbstractFirOldFrontendDiagnosticsTest
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirOldFrontendLightClassesTest : AbstractFirOldFrontendDiagnosticsTest() {
    override fun checkResultingFirFiles(firFiles: List<FirFile>, testDataFile: File) {
        super.checkResultingFirFiles(firFiles, testDataFile)

        val ourFinders =
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions.filterIsInstance<FirJavaElementFinder>()

        assertNotEmpty(ourFinders)

        val stringBuilder = StringBuilder()

        for (qualifiedName in InTextDirectivesUtils.findListWithPrefixes(testDataFile.readText(), "// LIGHT_CLASS_FQ_NAME: ")) {
            val fqName = FqName(qualifiedName)
            val packageName = fqName.parent().asString()

            val ourFinder = ourFinders.firstOrNull { finder -> finder.findPackage(packageName) != null }
            assertNotNull("PsiPackage for ${fqName.parent()} was not found", ourFinder)
            ourFinder!!

            val psiPackage = ourFinder.findPackage(fqName.parent().asString())
            assertNotNull("PsiPackage for ${fqName.parent()} is null", psiPackage)

            val psiClass = assertInstanceOf(
                ourFinder.findClass(qualifiedName, GlobalSearchScope.allScope(project)),
                ClsClassImpl::class.java
            )

            psiClass.appendMirrorText(0, stringBuilder)
            stringBuilder.appendln()
        }

        val expectedPath = testDataFile.path.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), stringBuilder.toString())
    }

    override fun createTestFileFromPath(filePath: String): File {
        return File(filePath)
    }
}
