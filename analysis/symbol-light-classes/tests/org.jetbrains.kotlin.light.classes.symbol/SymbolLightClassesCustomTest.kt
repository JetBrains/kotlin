/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics
import org.jetbrains.kotlin.analysis.api.javaInterop.asFacadePsiClass
import org.jetbrains.kotlin.analysis.api.javaInterop.asPsiClass
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.renderClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SymbolLightClassesCustomTest : AbstractAnalysisApiExecutionTest(testDirPathString = "testData/custom") {
    override val configurator = LLSourceLikeTestConfigurator()

    @Test
    fun fileModificationTracker(file: KtFile, testServices: TestServices) {
        analyze(file) {
            val facadeLightClass = file.symbol.asFacadePsiClass() ?: error("Facade light class was not found")
            val classLightClass =
                (file.declarations.first() as KtClassOrObject).classSymbol?.asPsiClass() ?: error("Light class was not found")
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
    }

    @Test
    fun enumEntryWithTypeAliasSameNameAsPrimitiveType(file: KtFile, testServices: TestServices) {
        val enumKtClass = file.declarations.filterIsInstance<KtClass>().first { it.isEnum() }
        val enumLightClass = analyze(file) {
            (enumKtClass.symbol as? KaClassSymbol)?.asPsiClass() ?: error("Light class was not found")
        }

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
        analyze(property) { property.diagnostics().toList() }
        assertMethodAnnotation(topLevelClass, testServices)
    }

    private fun assertMethodAnnotation(topLevelClass: KtClass, testServices: TestServices) {
        val topLevelLightClass = analyze(topLevelClass) {
            when (val symbol = topLevelClass.symbol) {
                is KaEnumEntrySymbol -> symbol.initializer?.asPsiClass()
                is KaClassSymbol -> symbol.asPsiClass()
                else -> null
            } ?: error("Light class was not found")
        }
        val method = topLevelLightClass.findMethodsByName("method", false).first() as PsiMethod
        val annotation = method.annotations.first()
        val argument = annotation.findAttributeValue("value")!! as PsiLiteralExpression
        testServices.assertions.assertEquals("my text", argument.value)

        val nameReference = (argument as KtLightElementBase).kotlinOrigin as KtNameReferenceExpression
        testServices.assertions.assertEquals("MY_CONST", nameReference.getReferencedName())
    }

    @Test
    fun findSuperMethods(file: KtFile) {
        val testFunction = file.declarations.findIsInstanceAnd<KtNamedFunction> { it.name == "test" }
            ?: error("Function 'test()' not found")

        analyze(file) {
            val javaImplSymbol = testFunction.valueParameters.single().symbol.returnType.symbol
                ?: error("'JavaImpl' parameter type not resolved")

            val psiJavaImplClass = javaImplSymbol.psi as PsiClass
            val psiFooMethod = psiJavaImplClass.findMethodsByName("foo", false).single()

            val psiFooMethodSupers = PsiSuperMethodImplUtil.findSuperMethods(psiFooMethod)

            assertEquals(1, psiFooMethodSupers.size)

            val psiJavaBaseClass = psiFooMethodSupers[0].parent as PsiClass
            assertEquals("lib.JavaBase", psiJavaBaseClass.qualifiedName)
        }
    }

    /**
     * A constructor is a default one only if the class declares no constructor at all, so the light class has to synthesize one.
     *
     * A deserialized `object` is such a case: the metadata stub of an object carries no primary constructor, so the object symbol has no
     * constructors either.
     *
     * Everything that does declare a constructor gets a regular light constructor. This includes the no-arg overload of a primary
     * constructor with default parameter values, even though it is represented by the very same
     * [SymbolLightNoArgConstructor][org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor] as the synthesized one.
     *
     * A regression test for KT-84373.
     */
    @Test
    fun isDefaultConstructor(file: KtFile, testServices: TestServices) {
        val project = file.project

        fun lightClassOf(name: String): PsiClass {
            val declaration = analyze(file) {
                val classId = ClassId(FqName("lib"), Name.identifier(name))
                val classSymbol = findClass(classId) ?: error("'$classId' symbol was not found")
                classSymbol.realPsi as? KtClassOrObject
                    ?: error("'$classId' is expected to have a decompiled PSI, but '${classSymbol.realPsi}' was found")
            }

            // Light classes for non-JVM declarations are only available with the multiplatform support enabled
            @OptIn(KaNonPublicApi::class)
            return withMultiplatformLightClassSupport(project) {
                declaration.toLightClass()
            } ?: error("Light class for '$name' was not found")
        }

        val libraryObjectLightClass = lightClassOf("LibraryObject")
        val synthesizedConstructor = libraryObjectLightClass.constructors.single()
        testServices.assertions.assertTrue(synthesizedConstructor.isDefaultConstructor) {
            "'LibraryObject' declares no constructor, so the synthesized '$synthesizedConstructor' is expected to be a default one"
        }

        testServices.assertions.assertEquals(
            expected = """
                public final class LibraryObject /* lib.LibraryObject*/ {
                  @org.jetbrains.annotations.NotNull()
                  public static final @org.jetbrains.annotations.NotNull() lib.LibraryObject INSTANCE;
    
                  private /* default ctor */  LibraryObject();//  .ctor()
                }
            """.trimIndent(),
            actual = libraryObjectLightClass.renderClass(),
        )

        val classesWithDeclaredConstructor = listOf(lightClassOf("LibraryClass")) +
                file.declarations.filterIsInstance<KtClassOrObject>().map { declaration ->
                    declaration.toLightClass() ?: error("Light class for '${declaration.name}' was not found")
                }

        val declaredConstructors = classesWithDeclaredConstructor.flatMap { it.constructors.asList() }
        for (constructor in declaredConstructors) {
            testServices.assertions.assertFalse(constructor.isDefaultConstructor) {
                "'${constructor.containingClass?.name}' declares a constructor, so '$constructor' is not expected to be a default one"
            }
        }

        // Otherwise the check above misses the no-arg overload of 'ClassWithDefaultParameterValues'
        testServices.assertions.assertTrue(declaredConstructors.any { it is SymbolLightNoArgConstructor }) {
            "A no-arg constructor overload is expected among $declaredConstructors"
        }
    }
}
