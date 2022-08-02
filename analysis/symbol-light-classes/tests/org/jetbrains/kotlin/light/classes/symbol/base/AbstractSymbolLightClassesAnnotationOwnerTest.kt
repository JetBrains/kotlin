/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import junit.framework.TestCase
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

abstract class AbstractSymbolLightClassesAnnotationOwnerTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val testDataFile = module.files.first {
            it.originalFile.extension in DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
        }.originalFile.toPath()

        val fqName = LightClassTestCommon.fqNameInTestDataFile(testDataFile.toFile())

        val ktFile = ktFiles.first()
        val lightClass = findLightClass(fqName, ktFile.project)

        lightClass?.accept(lightAnnotationVisitor)
    }

    override fun getRenderResult(ktFile: KtFile, testDataFile: Path, module: TestModule, project: Project): String {
        throw IllegalStateException("This test is not rendering light elements")
    }

    private val lightAnnotationVisitor = object : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass?) {
            aClass?.annotations?.forEach { it.accept(this) }

            aClass?.fields?.forEach { it.accept(this) }
            aClass?.methods?.forEach { it.accept(this) }
            aClass?.innerClasses?.forEach { it.accept(this) }

            aClass?.typeParameterList?.typeParameters?.forEach { p -> p.annotations.forEach { it.accept(this) } }
        }

        override fun visitField(field: PsiField?) {
            field?.annotations?.forEach { it.accept(this) }

            field?.type?.annotations?.forEach { it.accept(this) }
        }

        override fun visitMethod(method: PsiMethod?) {
            method?.annotations?.forEach { it.accept(this) }

            method?.returnType?.annotations?.forEach { it.accept(this) }
            method?.parameterList?.parameters?.forEach { p -> p.annotations.forEach { it.accept(this) } }
            method?.typeParameterList?.typeParameters?.forEach { p -> p.annotations.forEach { it.accept(this) } }
        }

        override fun visitTypeElement(type: PsiTypeElement?) {
            type?.annotations?.forEach { it.accept(this) }
        }

        override fun visitAnnotation(annotation: PsiAnnotation?) {
            if (annotation != null) {
                TestCase.assertNotNull(annotation.owner)
            }
        }
    }
}