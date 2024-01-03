/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiEnumConstantInitializer
import com.intellij.psi.search.GlobalSearchScope
import junit.framework.TestCase
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.asJava.renderClass
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightClassesFromFile
import org.jetbrains.kotlin.light.classes.symbol.base.service.withExtendedTypeRenderer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import java.nio.file.Path

abstract class AbstractSymbolLightClassesByPsiTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        val finder = JavaElementFinder.getInstance(project)
        val lightClasses = ktFiles.flatMap { getLightClassesFromFile(it) }
        if (lightClasses.isEmpty()) {
            // ROOT package exists
            TestCase.assertNotNull("ROOT package not found", finder.findPackage(""))
            return LightClassTestCommon.NOT_GENERATED_DIRECTIVE
        }
        val scope = GlobalSearchScope.allScope(project)
        for (lc in lightClasses) {
            val path = lc.containingFile.virtualFile.path
            val containingKtFile = ktFiles.find { it.virtualFilePath == path }
            TestCase.assertNotNull(containingKtFile)
            val packageFqName = containingKtFile?.packageDirective?.fqName ?: FqName.ROOT
            TestCase.assertNotNull("package $packageFqName not found", finder.findPackage(packageFqName.asString()))

            // [JavaElementFinder#findClass] finds facade classes and regular classes, not ones in a script.
            if (containingKtFile!!.isScript()) continue
            // Skip enum entries
            if (lc is PsiEnumConstantInitializer) continue

            val fqName = lc.qualifiedName ?: continue
            // [JavaElementFinder#findClass] declines to create a light class for invalid fqName.
            if (!isValidJavaFqName(fqName)) continue

            val lcViaFinder = finder.findClass(lc.qualifiedName!!, scope)
            TestCase.assertEquals(lc, lcViaFinder)
        }

        return withExtendedTypeRenderer(testDataFile) {
            lightClasses.sortedBy { it.qualifiedName ?: it.name.toString() }.joinToString("\n\n") { it.renderClass() }
        }
    }
}
