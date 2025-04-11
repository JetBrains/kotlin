/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import junit.framework.TestCase
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.ecj.EcjJavaClassFinder
import org.jetbrains.kotlin.fir.java.enhancement.AbstractJavaAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.fir.java.enhancement.FirEnhancedSymbolsStorage
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope.FirMappedSymbolStorage
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.TypeComponents
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.junit.Test
import java.nio.file.Files

/**
 * Tests for converting EcjJavaClass to FirJavaClass.
 */
class EcjJavaClassToFirTest : TestCase() {

    /**
     * Helper method that takes a Java source as a string and returns a FIR Java class.
     *
     * @param javaCode The Java source code as a string.
     * @param packageName The package name of the Java class.
     * @param className The name of the Java class.
     * @return The FirJavaClass representing the Java class.
     */
    private fun javaSourceToFir(
        javaCode: String,
        packageName: String = "test",
        className: String = "Test"
    ): FirJavaClass {
        // Create a temporary file with the Java source code
        val tempFile = Files.createTempFile("test", ".java").toFile()
        try {
            tempFile.writeText(javaCode)

            // Create an EcjJavaClassFinder with the temporary file
            val finder = EcjJavaClassFinder(listOf(tempFile))

            // Find the class
            val classId = ClassId(FqName(packageName), FqName(className), false)
            val ecjJavaClass = finder.findClass(classId)
                ?: throw IllegalArgumentException("Class not found: $classId")

            // Create a simple FirSession for testing
            val session = createTestSession()
            val moduleData = session.moduleData

            // Create a FirRegularClassSymbol for the class
            val classSymbol = FirRegularClassSymbol(classId)

            // Convert the EcjJavaClass to a FirJavaClass
            return ecjJavaClass.convertJavaClassToFir(classSymbol, null, moduleData)
        } finally {
            // Clean up the temporary file
            tempFile.delete()
        }
    }

    /**
     * Creates a simple FirSession for testing.
     */
    @OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
    private fun createTestSession(): FirSession {
        return object : FirSession(null, Kind.Source) {}.apply {
            val moduleData = object : FirModuleData() {
                override val name: Name = Name.identifier("<test module>")
                override val dependencies: List<FirModuleData> = emptyList()
                override val dependsOnDependencies: List<FirModuleData> = emptyList()
                override val allDependsOnDependencies: List<FirModuleData> = emptyList()
                override val friendDependencies: List<FirModuleData> = emptyList()
                override val platform = JvmPlatforms.unspecifiedJvmPlatform
                override val isCommon: Boolean = false
                override val session: FirSession
                    get() = boundSession ?: error("Session not bound")
                override val stableModuleName: String? = null
            }

            register(FirModuleData::class, moduleData)
            moduleData.bindSession(this)
            register(FirCachesFactory::class, FirThreadUnsafeCachesFactory)
            register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            register(TypeComponents::class, TypeComponents(this))
            register(FirExtensionService::class, FirExtensionService(this))
            val javaTypeEnhancementState = languageVersionSettings.getFlag(JvmAnalysisFlags.javaTypeEnhancementState)
            register(
                AbstractJavaAnnotationTypeQualifierResolver::class,
                DummyFirAnnotationTypeQualifierResolver(this, javaTypeEnhancementState)
            )
            register(FirEnhancedSymbolsStorage::class, FirEnhancedSymbolsStorage(this))
            register(FirMappedSymbolStorage::class, FirMappedSymbolStorage(this))
            val symbolProvider = FirCachingCompositeSymbolProvider(this, emptyList())
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    @Test
    fun testSimpleClass() {
        val javaCode = """
            package test;

            public class Test {
                public void publicMethod() {}
                private void privateMethod() {}
                public int publicField;
                private int privateField;
            }
        """.trimIndent()

        val firJavaClass = javaSourceToFir(javaCode)
        val renderedFir = firJavaClass.render()

        // Verify that the rendered FIR contains the expected elements
        assertTrue("FIR should contain class name", renderedFir.contains("class Test"))
        assertTrue("FIR should have ClassKind.CLASS", renderedFir.contains("classKind: CLASS"))
        assertTrue("FIR should be public", renderedFir.contains("visibility: public"))
    }

    @Test
    fun testInterface() {
        val javaCode = """
            package test;

            public interface Test {
                void method1();
                void method2();
                int CONSTANT = 42;
            }
        """.trimIndent()

        val firJavaClass = javaSourceToFir(javaCode)
        val renderedFir = firJavaClass.render()

        // Verify that the rendered FIR contains the expected elements
        assertTrue("FIR should contain interface name", renderedFir.contains("class Test"))
        assertTrue("FIR should have ClassKind.INTERFACE", renderedFir.contains("classKind: INTERFACE"))
        assertTrue("FIR should be public", renderedFir.contains("visibility: public"))
    }

    @Test
    fun testEnum() {
        val javaCode = """
            package test;

            public enum Test {
                ONE, TWO, THREE;

                public void method() {}
            }
        """.trimIndent()

        val firJavaClass = javaSourceToFir(javaCode)
        val renderedFir = firJavaClass.render()

        // Verify that the rendered FIR contains the expected elements
        assertTrue("FIR should contain enum name", renderedFir.contains("class Test"))
        assertTrue("FIR should have ClassKind.ENUM_CLASS", renderedFir.contains("classKind: ENUM_CLASS"))
        assertTrue("FIR should be public", renderedFir.contains("visibility: public"))
    }

    @Test
    fun testAnnotation() {
        val javaCode = """
            package test;

            public @interface Test {
                String value() default "";
                int count() default 0;
            }
        """.trimIndent()

        val firJavaClass = javaSourceToFir(javaCode)
        val renderedFir = firJavaClass.render()

        // Verify that the rendered FIR contains the expected elements
        assertTrue("FIR should contain annotation name", renderedFir.contains("class Test"))
        assertTrue("FIR should have ClassKind.ANNOTATION_CLASS", renderedFir.contains("classKind: ANNOTATION_CLASS"))
        assertTrue("FIR should be public", renderedFir.contains("visibility: public"))
    }
}

class DummyFirAnnotationTypeQualifierResolver(
    session: FirSession,
    javaTypeEnhancementState: JavaTypeEnhancementState,
) :
    AbstractJavaAnnotationTypeQualifierResolver(session, javaTypeEnhancementState)
{
    override fun extractDefaultQualifiers(firClass: FirRegularClass): JavaTypeQualifiersByElementType? = null
}
