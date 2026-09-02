/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirBinaryDependenciesModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/**
 * Implicit supertypes (`java.lang.Object`, `java.lang.Enum`, `java.lang.annotation.Annotation`,
 * `java.lang.Record`; JLS 8.1.4 / 8.9 / 9.1.3 / 8.10) are supplied by the compiler under their
 * canonical names, so resolving them must be a single existence probe of that `ClassId` — not a
 * JLS 6.5 scope lookup in the declaring file. The scope lookup classified the leading `java`
 * segment against member types first, probing e.g. `Outer.java` through the symbol provider (an
 * extra `FindJavaClass` hit in the CLI perf report) and letting a member type named `java` shadow
 * the supertype.
 */
class JavaImplicitSupertypeResolutionTest : JavaParsingTestBase() {
    private val javaLangObject = ClassId.topLevel(FqName("java.lang.Object"))
    private val javaLangEnum = ClassId.topLevel(FqName("java.lang.Enum"))
    private val javaLangRecord = ClassId.topLevel(FqName("java.lang.Record"))

    /** Session whose symbol provider knows exactly [known] and records every probed [ClassId]. */
    @OptIn(SessionConfiguration::class)
    private fun sessionWithRecordingProvider(known: Set<ClassId>, probes: MutableList<ClassId>): FirSession {
        val session = createDummyFirSessionForTests()
        session.register(FirSymbolProvider::class, StubSymbolProvider(session) { classId ->
            probes.add(classId)
            if (classId in known) libraryClassSymbol(classId) else null
        })
        return session
    }

    private fun libraryClassSymbol(classId: ClassId): FirRegularClassSymbol = buildRegularClass {
        moduleData = FirBinaryDependenciesModuleData(Name.special("<test>"))
        origin = FirDeclarationOrigin.Library
        name = classId.shortClassName
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
        classKind = ClassKind.CLASS
        scopeProvider = JavaScopeProvider
        symbol = FirRegularClassSymbol(classId)
    }.symbol

    private fun assertSingleCanonicalProbe(source: String, expected: ClassId, alsoKnown: Set<ClassId> = emptySet()) {
        val probes = mutableListOf<ClassId>()
        val javaClass = parseFirstClass(source, sessionWithRecordingProvider(alsoKnown + expected, probes))

        val supertype = javaClass.supertypes.single()
        assertEquals(expected.asSingleFqName().asString(), supertype.classifierQualifiedName)
        assertEquals(expected, assertInstanceOf(JavaClass::class.java, supertype.classifier).classId)
        assertEquals(listOf(expected), probes, "Only the canonical ClassId may be probed")
    }

    @Test
    fun testImplicitObjectSupertypeIsSingleCanonicalProbe() {
        assertSingleCanonicalProbe("public class Outer {}", javaLangObject)
    }

    @Test
    fun testImplicitEnumSupertypeIsSingleCanonicalProbe() {
        assertSingleCanonicalProbe("public enum E { A }", javaLangEnum)
    }

    @Test
    fun testImplicitRecordSupertypeIsSingleCanonicalProbe() {
        assertSingleCanonicalProbe("public record R(int x) {}", javaLangRecord)
    }

    @Test
    fun testImplicitObjectSupertypeIsNotShadowedByMemberType() {
        // In scope, `java.lang.Object` would denote `Outer.java.lang.Object` (JLS 6.5.4: member
        // types shadow packages); the implicit supertype must stay `java.lang.Object`.
        val source = """
            public class Outer {
                public static class java {
                    public static class lang {
                        public static class Object {}
                    }
                }
            }
        """.trimIndent()
        val shadowing = listOf("Outer.java", "Outer.java.lang", "Outer.java.lang.Object")
            .map { ClassId(FqName.ROOT, FqName(it), isLocal = false) }
        assertSingleCanonicalProbe(source, javaLangObject, alsoKnown = shadowing.toSet())
    }
}
