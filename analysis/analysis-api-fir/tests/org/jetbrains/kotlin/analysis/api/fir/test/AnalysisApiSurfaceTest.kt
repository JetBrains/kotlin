/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.analyzeCopy
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirPrimaryConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.org.objectweb.asm.Type
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun deprecatedMapToJvmType(mainFile: KtFile) {
        val testFunction = mainFile.declarations.firstIsInstance<KtFunction>()
        assertEquals("test", testFunction.name)

        analyze(testFunction) {
            val valueParameter = testFunction.valueParameters.single()

            val kotlinType = valueParameter.symbol.returnType

            val memberMethod = this@analyze::class.java
                .getMethod("mapToJvmType", KaType::class.java, TypeMappingMode::class.java)

            assert(memberMethod.isSynthetic)

            val contextParameterBridgeMethod = Class
                .forName("org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponentKt")
                .getMethod("mapToJvmType", KaSession::class.java, KaType::class.java, TypeMappingMode::class.java)

            assert(contextParameterBridgeMethod.isSynthetic)

            val expectedResult = Type.getType("LFoo;")

            val memberResult = memberMethod.invoke(this@analyze, kotlinType, TypeMappingMode.DEFAULT)
            assertEquals(expectedResult, memberResult)

            val contextParameterBridgeResult = contextParameterBridgeMethod.invoke(null, this@analyze, kotlinType, TypeMappingMode.DEFAULT)
            assertEquals(expectedResult, contextParameterBridgeResult)
        }
    }

    @Test
    fun deprecatedFunctionTypeKind(mainFile: KtFile) {
        val testFunction = mainFile.declarations.firstIsInstance<KtFunction>()
        assertEquals("test", testFunction.name)

        analyze(testFunction) {
            val valueParameter = testFunction.valueParameters.single()

            val kotlinType = valueParameter.symbol.returnType

            val memberMethod = this@analyze::class.java
                .getMethod("getFunctionTypeKind", KaType::class.java)

            // For some reason, getters of HIDDEN-deprecated interface properties aren't synthetic
            assert(!memberMethod.isSynthetic)

            val contextParameterBridgeMethod = Class
                .forName("org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProviderKt")
                .getMethod("getFunctionTypeKind", KaSession::class.java, KaType::class.java)

            assert(contextParameterBridgeMethod.isSynthetic)

            val expectedResult = FunctionTypeKind.Function

            val memberResult = memberMethod.invoke(this@analyze, kotlinType)
            assertEquals(expectedResult, memberResult)

            val contextParameterBridgeResult = contextParameterBridgeMethod.invoke(null, this@analyze, kotlinType)
            assertEquals(expectedResult, contextParameterBridgeResult)
        }
    }

    @Test
    fun deprecatedAnnotationApplicableTargets(mainFile: KtFile) {
        val annotationClass = mainFile.declarations.firstIsInstance<KtClass>()
        assertEquals("MyAnnotation", annotationClass.name)

        analyze(annotationClass) {
            val classSymbol = annotationClass.classSymbol!!

            val memberMethod = this@analyze::class.java
                .getMethod("getAnnotationApplicableTargets", KaClassSymbol::class.java)

            // For some reason, getters of HIDDEN-deprecated interface properties aren't synthetic
            assert(!memberMethod.isSynthetic)

            val contextParameterBridgeMethod = Class
                .forName("org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProviderKt")
                .getMethod("getAnnotationApplicableTargets", KaSession::class.java, KaClassSymbol::class.java)

            assert(contextParameterBridgeMethod.isSynthetic)

            @Suppress("UNCHECKED_CAST")
            val memberResult = memberMethod.invoke(this@analyze, classSymbol) as Set<KotlinTarget>
            assert(KotlinTarget.CLASS in memberResult)
            assert(KotlinTarget.FUNCTION in memberResult)

            @Suppress("UNCHECKED_CAST")
            val contextParameterBridgeResult = contextParameterBridgeMethod.invoke(null, this@analyze, classSymbol) as Set<KotlinTarget>
            assertEquals(memberResult, contextParameterBridgeResult)
        }
    }
}
