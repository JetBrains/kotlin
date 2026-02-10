/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiArrayType
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import com.intellij.psi.PsiEnumConstant
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test

class SymbolLightClassesCustomTest : AbstractAnalysisApiExecutionTest(
    testDirPathString = "analysis/symbol-light-classes/testData/custom",
) {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun fileModificationTracker(file: KtFile, testServices: TestServices) {
        val facadeLightClass = file.findFacadeClass() ?: error("Facade light class was not found")
        val classLightClass = (file.declarations.first() as KtClassOrObject).toLightClass() ?: error("Light class was not found")
        val fakeFilesWithModificationStamp = listOf(facadeLightClass, classLightClass).map { lightClass ->
            lightClass.containingFile to lightClass.containingFile.modificationStamp
        }

        // Emulate file modification
        file.clearCaches()

        for ((fakeFile, originalStamp) in fakeFilesWithModificationStamp) {
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

    @Test
    fun enumValuesReturnTypeElementNullability(file: KtFile, testServices: TestServices) {
        val enumKtClass = file.declarations.filterIsInstance<KtClass>().first { it.isEnum() }
        val enumLightClass = enumKtClass.toLightClass() ?: error("Light class was not found")

        val valuesMethod = enumLightClass.methods.single { method -> method.name == "values" && method.parameterList.parametersCount == 0 }
        val returnType = valuesMethod.returnType as? PsiArrayType ?: error("Expected PsiArrayType for enum values()")
        val componentType = returnType.componentType

        val notNullQualifiedName = JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION.asString()
        val hasNotNullOnComponent = componentType.annotations.any { annotation -> annotation.hasQualifiedName(notNullQualifiedName) }
        testServices.assertions.assertTrue(hasNotNullOnComponent) {
            "Expected enum values() component type to have @$notNullQualifiedName, but got ${componentType.annotations.toList()}"
        }
    }
}
