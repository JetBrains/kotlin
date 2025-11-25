/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesTestBase
import org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators.AnalysisApiSymbolLightClassesDecompiledTestConfigurator
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addIfNotNull
import java.nio.file.Path
import kotlin.io.path.readText

abstract class AbstractSymbolLightClassesMatcherByPsiForLibraryTest :
    AbstractSymbolLightClassesTestBase(AnalysisApiSymbolLightClassesDecompiledTestConfigurator(JvmPlatforms.defaultJvmPlatform)) {

    override val isTestAgainstCompiledCode: Boolean = true

    override val currentExtension: String
        get() = throw IllegalStateException("Should not be called")

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doLightClassTest(
        ktFiles: List<KtFile>,
        module: KtTestModule,
        testServices: TestServices,
    ) {
        val declarationsWithoutLightElementsNames = mutableSetOf<String>()
        val lightElementsWithoutDeclarationNames = mutableSetOf<String>()
        for (file in ktFiles) {
            val javaSupport = KotlinAsJavaSupport.getInstance(file.project)
            testServices.assertions.assertTrue(file is KtClsFile)
            file as KtClsFile
            val declarations = mutableMapOf<KtDeclaration, Boolean>()
            val lightElementsWithoutDeclaration = mutableSetOf<PsiMember>()
            fun collectDeclarations(container: KtDeclarationContainer) {
                container.declarations.forEach { declaration ->
                    declarations[declaration] = false
                    if (declaration is KtClassOrObject) {
                        declaration.primaryConstructor?.let { declarations[it] = false }
                    }
                    if (declaration is KtDeclarationContainer) {
                        collectDeclarations(declaration)
                    }
                }
            }

            collectDeclarations(file)
            file.declarations.filterIsInstance<KtClassOrObject>().forEach { classOrObject ->
                val lightClass = javaSupport.getLightClass(classOrObject)
                if (lightClass != null) {
                    processLightClass(lightClass, file, declarations, lightElementsWithoutDeclaration, testServices)
                }
            }
            javaSupport.getLightFacade(file)?.let { lightFacade ->
                processLightClass(lightFacade, file, declarations, lightElementsWithoutDeclaration, testServices)
            }

            val names = declarations.filter { !it.value && it.key !is KtClassOrObject && it.key !is KtConstructor<*> }
                .mapNotNull { it.key.name }
            if (names.isNotEmpty()) {
                declarationsWithoutLightElementsNames.add(
                    file.name + names.sorted().joinToString(prefix = "[", postfix = "]", separator = ";")
                )
            }

            val lightMembersNames = lightElementsWithoutDeclaration.mapNotNull { it.name }
            if (lightMembersNames.isNotEmpty()) {
                lightElementsWithoutDeclarationNames.add(
                    file.name + lightMembersNames.sorted().joinToString(prefix = "[", postfix = "]", separator = ";")
                )
            }
        }

        val text = testDataPath.readText()
        val prefix1 = "// ${Directives::DECLARATIONS_NO_LIGHT_ELEMENTS.name}:"
        val actualValue1 = declarationsWithoutLightElementsNames.toList().sorted().joinToString(separator = ", ")
        val prefix2 = "// ${Directives::LIGHT_ELEMENTS_NO_DECLARATION.name}:"
        val actualValue2 = lightElementsWithoutDeclarationNames.toList().sorted().joinToString(separator = ", ")
        val result = text.modifyText(prefix1, actualValue1).modifyText(prefix2, actualValue2)
        testServices.assertions.assertEqualsToFile(testDataPath.toFile(), result)
    }

    fun String.modifyText(prefix: String, actualValue: String): String {
        val lines = this.lines()
        val actualLine = "$prefix $actualValue"
        val index = lines.indexOfFirst { it.startsWith(prefix) }
        return when {
            actualValue.isEmpty() -> {
                if (index < 0) this else lines.filterNot { it.startsWith(prefix) }.joinToString("\n")
            }

            index >= 0 -> {
                val mutable = lines.toMutableList()
                mutable[index] = actualLine
                mutable.joinToString("\n")
            }

            else -> "$this\n$actualLine"
        }
    }

    private fun processLightClass(
        lightClass: PsiClass,
        file: KtClsFile,
        ktDeclarations: MutableMap<KtDeclaration, Boolean>,
        lightElementsWithoutDeclaration: MutableSet<PsiMember>,
        testServices: TestServices,
    ) {
        val compiledFileSearcher = KotlinDeclarationInCompiledFileSearcher.getInstance()

        fun checkMember(method: PsiMember) {
            val decl = compiledFileSearcher.findDeclarationInCompiledFile(file, method)
            if (decl != null && ktDeclarations.contains(decl)) {
                ktDeclarations[decl] = true
            } else {
                lightElementsWithoutDeclaration.add(method)
            }
        }

        lightClass.methods.forEach { method ->
            checkMember(method)
        }

        val innerClassesToIgnore = mutableSetOf<String>()
        //do not check generated synthetics, IDE should not resolve there
        innerClassesToIgnore.addIfNotNull("DefaultImpls")

        lightClass.fields.forEach { field ->
            // do not check light elements inside enum constants
            innerClassesToIgnore.addIfNotNull((field as? PsiEnumConstant)?.name)
            checkMember(field)
        }

        lightClass.innerClasses.forEach { innerClass ->
            if (innerClass.name in innerClassesToIgnore) {
                return@forEach
            }
            processLightClass(innerClass, file, ktDeclarations, lightElementsWithoutDeclaration, testServices)
        }
    }

    override fun getRenderResult(
        ktFile: KtFile,
        ktFiles: List<KtFile>,
        testDataFile: Path,
        module: KtTestModule,
        project: Project,
    ): String {
        throw IllegalStateException("This test is not rendering light elements")
    }

    private object Directives : SimpleDirectivesContainer() {
        val DECLARATIONS_NO_LIGHT_ELEMENTS by stringDirective(
            description = "Enumerate all declarations in kotlin file without light elements"
        )

        val LIGHT_ELEMENTS_NO_DECLARATION by stringDirective(
            description = "Enumerate all generated light members without declarations to navigate to"
        )
    }
}