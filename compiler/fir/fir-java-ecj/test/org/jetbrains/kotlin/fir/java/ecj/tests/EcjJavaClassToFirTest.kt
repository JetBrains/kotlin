/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassKind
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
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for converting EcjJavaClass to FirJavaClass.
 */
class EcjJavaClassToFirTest {

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

        val expectedFir = """
            public open class Test : R|kotlin/Any| {
                public open fun publicMethod(): R|kotlin/Unit|

                public field publicField: R|kotlin/Int|

            }
        """.trimIndent()

        val firJavaClass = javaSourceToFir(javaCode)
        val renderedFir = firJavaClass.render().trim()

        assertEquals(ClassKind.CLASS, firJavaClass.classKind, "FIR should have ClassKind.CLASS")
        assertEquals(expectedFir, renderedFir)
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
        val renderedFir = firJavaClass.render().trim()

        val expectedFir = """
            public abstract interface Test : R|kotlin/Any| {
                public/*package*/ open fun method1(): R|kotlin/Unit|

                public/*package*/ open fun method2(): R|kotlin/Unit|

                public/*package*/ field CONSTANT: R|kotlin/Int|

            }
        """.trimIndent()

        assertEquals(expectedFir, renderedFir)
        assertEquals(ClassKind.INTERFACE, firJavaClass.classKind)
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
        val renderedFir = firJavaClass.render().trim()

        val expectedFir = """
                public final enum class Test : R|kotlin/Any| {
                    public final static fun values(): R|kotlin/Array<test/Test>| {
                    }

                    public final static fun valueOf(value: R|kotlin/String|): R|test/Test| {
                    }

                    public final static val entries: R|kotlin/enums/EnumEntries<test/Test>|
                        public get(): R|kotlin/enums/EnumEntries<test/Test>|

                    public final static enum entry ONE: R|test/Test|
                    public final static enum entry TWO: R|test/Test|
                    public final static enum entry THREE: R|test/Test|
                    public open fun method(): R|kotlin/Unit|

                }
            """.trimIndent()

        // Use the same comparison method as testSimpleClass
        assertEquals(expectedFir, renderedFir)
        assertEquals(ClassKind.ENUM_CLASS, firJavaClass.classKind)
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
        val renderedFir = firJavaClass.render().trim()

        val expectedFir = """
            public abstract annotation class Test : R|kotlin/Any| {
                public/*package*/ open fun value(): R|kotlin/Unit|

                public/*package*/ open fun count(): R|kotlin/Unit|

            }
        """.trimIndent()

        assertEquals(expectedFir, renderedFir)
        assertEquals(ClassKind.ANNOTATION_CLASS, firJavaClass.classKind)
    }

    @Test
    fun testNestedClasses() {
        val javaCode = """
            package test;

            public class Test {
                public class NestedClass {
                    public void nestedMethod() {}
                    public int nestedField;
                }

                public interface NestedInterface {
                    void interfaceMethod();
                    int INTERFACE_CONSTANT = 42;
                }
            }
        """.trimIndent()

        val firJavaClass = javaSourceToFir(javaCode)
        val renderedFir = firJavaClass.render().trim()

        val expectedFir = """
            public open class Test : R|kotlin/Any| {
                public open inner class NestedClass : R|kotlin/Any| {
                    public open fun nestedMethod(): R|kotlin/Unit|

                    public field nestedField: R|kotlin/Int|

                }

                public abstract inner interface NestedInterface : R|kotlin/Any| {
                    public/*package*/ open fun interfaceMethod(): R|kotlin/Unit|

                    public/*package*/ field INTERFACE_CONSTANT: R|kotlin/Int|

                }

            }
        """.trimIndent()

        assertEquals(expectedFir, renderedFir)
        assertEquals(ClassKind.CLASS, firJavaClass.classKind)
    }
}

class DummyFirAnnotationTypeQualifierResolver(
    session: FirSession,
    javaTypeEnhancementState: JavaTypeEnhancementState?,
) :
    AbstractJavaAnnotationTypeQualifierResolver(
        session,
        javaTypeEnhancementState ?: JavaTypeEnhancementState.getDefault(KotlinVersion(2, 1, 0))
    )
{
    override fun extractDefaultQualifiers(firClass: FirRegularClass): JavaTypeQualifiersByElementType? = null
}
