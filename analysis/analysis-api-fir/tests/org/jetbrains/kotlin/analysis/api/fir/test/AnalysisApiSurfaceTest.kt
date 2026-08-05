/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirPrimaryConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.javaInterop.namedClassSymbol
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.scopes.staticMemberScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.analyzeCopy
import org.jetbrains.kotlin.analysis.api.session.canBeAnalysed
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AnalysisApiSurfaceTest : AbstractAnalysisApiExecutionTest("testData/surface") {
    override val configurator = LLSourceLikeTestConfigurator()

    @Test
    fun supertypeIteration(mainFile: KtFile) {
        val implClass = mainFile.declarations.first { it is KtClass && it.name == "Impl" } as KtClass
        analyze(implClass) {
            val defaultClassType = implClass.classSymbol!!.defaultType

            val allSupertypeSequence = defaultClassType.allSupertypes
            val directSupertypeSequence = defaultClassType.directSupertypes

            // Iterate through the sequence multiple times
            assertEquals(allSupertypeSequence.toList(), allSupertypeSequence.toList())
            assertEquals(directSupertypeSequence.toList(), directSupertypeSequence.toList())
        }
    }

    @Test
    fun enumEntryWithBodyConstructorPointerInIgnoreSelfMode(mainFile: KtFile) {
        // Create a file copy to set up resolution in IGNORE_SELF mode
        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = true)
        val fileCopy = ktPsiFactory.createFile("copy.kt", mainFile.text)
        fileCopy.originalFile = mainFile

        val enumClass = fileCopy.declarations.filterIsInstance<KtClass>().first()
        analyzeCopy(enumClass, KaDanglingFileResolutionMode.IGNORE_SELF) {
            val enumSymbol = enumClass.classSymbol as KaNamedClassSymbol
            val enumEntrySymbol = enumSymbol.staticMemberScope.callables
                .filterIsInstance<KaEnumEntrySymbol>()
                .single()

            // Get the implicit primary constructor
            val constructor = enumEntrySymbol.initializer!!.memberScope.constructors.toList().single()

            val pointer = constructor.createPointer()
            assertIs<KaFirPrimaryConstructorSymbolPointer>(pointer, "Expected primary constructor pointer")

            val restored = pointer.restoreSymbol()
            assertEquals(constructor, restored, "Constructor pointer should be restored to original constructor")
        }
    }

    @Test
    fun javaTypeParameterSubstitutor(mainFile: KtFile) {
        analyze(mainFile) {
            fun checkSubstitution(classSymbol: KaNamedClassSymbol) {
                val substitutor = buildSubstitutor {
                    substitution(classSymbol.typeParameters.single(), builtinTypes.int)
                }
                val substitutedType = substitutor.substitute(classSymbol.defaultType) as KaClassType
                val typeArgument = substitutedType.typeArguments.single()
                assertEquals(builtinTypes.int, typeArgument.type, "Type argument should be Int after substitution")
            }

            val regularClassSymbol = findClass(ClassId.fromString("test/JavaBox")) as KaFirPsiJavaClassSymbol
            val psiBasedClassSymbol = (regularClassSymbol.psi as PsiClass).namedClassSymbol as KaFirPsiJavaClassSymbol
            assertEquals(regularClassSymbol, psiBasedClassSymbol)

            assertIs<KaFirPsiJavaTypeParameterSymbol>(regularClassSymbol.typeParameters.single())
            assertIs<KaFirPsiJavaTypeParameterSymbol>(psiBasedClassSymbol.typeParameters.single())

            checkSubstitution(regularClassSymbol)
            checkSubstitution(psiBasedClassSymbol)
        }
    }

    @Test
    fun substitutedAnnotation(testServices: TestServices) {
        val allFiles = testServices.ktTestModuleStructure.allSourceFiles
        val commonFile = allFiles.single { it.name == "myFacade.kt" } as KtFile
        val jvmFile = allFiles.single { it.name == "main.kt" } as KtFile

        val project = commonFile.project
        val facadeClass = JavaPsiFacade.getInstance(project).findClass(
            "mypack.MyFacadeKt",
            GlobalSearchScope.projectScope(project),
        )

        assertNotNull(facadeClass, "'mypack.MyFacadeKt' light class is not found")

        val facadeMethod = facadeClass.methods.single()

        assertEquals("myCustomName", facadeMethod.name)
        val psiAnnotation = facadeMethod.annotations.single()

        assertEquals("kotlin.jvm.JvmName", psiAnnotation.qualifiedName)

        analyze(jvmFile) {
            val function = commonFile.declarations.single() as KtNamedFunction
            val functionSymbol = function.symbol
            val annotation = functionSymbol.annotations.single()
            val constructorSymbol = annotation.constructorSymbol ?: error("The constructor symbol is absent")

            // BUG! It has to point to the JVM Stdlib. JVM session cannot depend on classes from klib
            assertEquals("Library kotlin-stdlib-metadata", constructorSymbol.containingModule.moduleDescription)
            val constructorPsi = constructorSymbol.realPsi ?: error("The real psi is not found")

            // BUG! It has to be analyzable
            assertFalse(constructorPsi.canBeAnalysed())
        }
    }
}
