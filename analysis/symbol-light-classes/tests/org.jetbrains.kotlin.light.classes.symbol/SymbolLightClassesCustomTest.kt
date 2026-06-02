/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test

class SymbolLightClassesCustomTest : AbstractAnalysisApiExecutionTest(testDirPathString = "testData/custom") {
    override val configurator = LLSourceLikeTestConfigurator()

    @Test
    fun fileModificationTracker(file: KtFile, testServices: TestServices) {
        val facadeLightClass = file.findFacadeClass() ?: error("Facade light class was not found")
        val classLightClass = (file.declarations.first() as KtClassOrObject).toLightClass() ?: error("Light class was not found")
        val fakeFilesWithModificationStamp = listOf(facadeLightClass, classLightClass).map { lightClass ->
            lightClass.containingFile to lightClass.containingFile.modificationStamp
        }

        // Emulate file modification
        file.clearCaches()

        for ([fakeFile, originalStamp] in fakeFilesWithModificationStamp) {
            val newStamp = fakeFile.modificationStamp
            testServices.assertions.assertTrue(originalStamp < newStamp) {
                "Expected that $fakeFile will have a modification stamp greater than $originalStamp, but $newStamp was found"
            }
        }
    }

    @Test
    fun enumEntryWithTypeAliasSameNameAsPrimitiveType(file: KtFile, testServices: TestServices) {
        val enumKtClass = file.declarations.filterIsInstance<KtClass>().first { it.isEnum() }
        val enumLightClass = enumKtClass.toLightClass() ?: error("Light class was not found")

        val enumConstant = enumLightClass.fields.filterIsInstance<PsiEnumConstant>().first()
        val enumConstantType = enumConstant.type as PsiClassType
        val actualEnumConstantClass = enumConstantType.resolve()

        testServices.assertions.assertEquals(enumLightClass, actualEnumConstantClass) {
            "Expected enum constant's type to resolve to the enum class, but got $actualEnumConstantClass"
        }

        val enumEntry = enumConstant.initializingClass as SymbolLightClassForEnumEntry
        val baseClassType = enumEntry.baseClassType
        val actualBaseClass = baseClassType.resolve()

        testServices.assertions.assertEquals(enumLightClass, actualBaseClass) {
            "Expected enum entry's base type to resolve to the enum class, but got $actualBaseClass"
        }
    }

    /**
     * A regression test for KT-83766 to ensure that annotation arguments have an argument PSI element
     */
    @Test
    fun annotationArgumentPsi(file: KtFile, testServices: TestServices) {
        val topLevelClass = file.declarations.first() as KtClass
        assertMethodAnnotation(topLevelClass, testServices)
    }

    /**
     * A regression test for KT-83766 to ensure that annotation arguments have an argument PSI element
     */
    @Test
    fun annotationArgumentPsiPreresolved(file: KtFile, testServices: TestServices) {
        val topLevelClass = file.declarations.first() as KtClass
        val companion = topLevelClass.declarations.last() as KtObjectDeclaration
        val property = companion.declarations.first() as KtProperty

        // Trigger full body resolve for property. This is crucial to resolve only the property first
        analyze(property) { property.directDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS) }
        assertMethodAnnotation(topLevelClass, testServices)
    }

    /**
     * A reproducer for KT-60993.
     *
     * The Kotlin compiler marks the accessors and the backing field of a property annotated with [kotlin.Deprecated]
     * with the JVM `Deprecated` attribute (see `compiler/testData/codegen/bytecodeListing/javaDeprecated.txt` and the
     * `ACC_DEPRECATED` handling in `FunctionCodegen`). When such bytecode is read back into Java PSI, that attribute is
     * surfaced as a synthetic `@java.lang.Deprecated` annotation. Symbol Light Classes are expected to mirror the
     * bytecode, but currently they do not add `@java.lang.Deprecated` to the accessors and the backing field, even
     * though [com.intellij.psi.PsiDocCommentOwner.isDeprecated] already returns `true` for them.
     *
     * This test pins the current (buggy) behavior. Once KT-60993 is fixed, flip the `assertFalse` checks below to
     * `assertTrue` (the `isDeprecated` checks should keep passing).
     */
    @Test
    fun deprecatedPropertyJavaLangDeprecated(file: KtFile, testServices: TestServices) {
        val ktClass = file.declarations.first() as KtClass
        val lightClass = ktClass.toLightClass() ?: error("Light class was not found")

        val getter = lightClass.findMethodsByName("getX", false).single()
        val setter = lightClass.findMethodsByName("setX", false).single()
        val field = lightClass.fields.single { it.name == "x" }

        val members = listOf<Pair<String, PsiModifierListOwner>>(
            "getter 'getX'" to getter,
            "setter 'setX'" to setter,
            "backing field 'x'" to field,
        )

        for ([description, member] in members) {
            testServices.assertions.assertTrue((member as PsiDocCommentOwner).isDeprecated) {
                "Expected the $description of a @Deprecated property to be deprecated"
            }

            // KT-60993: should become `assertTrue` once the accessors and the backing field are marked
            // with @java.lang.Deprecated.
            testServices.assertions.assertFalse(member.modifierList?.hasAnnotation("java.lang.Deprecated") == true) {
                "KT-60993: the $description of a @Deprecated property is not yet marked with @java.lang.Deprecated, " +
                        "but the annotation was found"
            }
        }
    }

    private fun assertMethodAnnotation(topLevelClass: KtClass, testServices: TestServices) {
        val topLevelLightClass = topLevelClass.toLightClass() ?: error("Light class was not found")
        val method = topLevelLightClass.findMethodsByName("method", false).first() as PsiMethod
        val annotation = method.annotations.first()
        val argument = annotation.findAttributeValue("value")!! as PsiLiteralExpression
        testServices.assertions.assertEquals("my text", argument.value)

        val nameReference = (argument as KtLightElementBase).kotlinOrigin as KtNameReferenceExpression
        testServices.assertions.assertEquals("MY_CONST", nameReference.getReferencedName())
    }
}
