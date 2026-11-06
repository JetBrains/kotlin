/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.java.direct.modules.readJavaModuleInfoSource
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

/**
 * [readJavaModuleInfoSource] reads the name, `requires`, `exports` and module annotations of a `module-info.java`,
 * ignores `opens`/`uses`/`provides`, resolves annotations through the classpath lookup, and returns `null`
 * for a text without a module declaration.
 */
class JavaModuleInfoSourceReaderTest {
    private val nullMarked = ClassId.topLevel(FqName("org.jspecify.annotations.NullMarked"))
    private val deprecated = ClassId.topLevel(FqName("java.lang.Deprecated"))

    private fun stubClass(classId: ClassId): JavaClass =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(JavaClass::class.java)) { proxy, method, args ->
            when (method.name) {
                "getClassId" -> classId
                "getFqName" -> classId.asSingleFqName()
                "getName" -> classId.shortClassName
                "toString" -> "StubJavaClass($classId)"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args[0]
                else -> throw UnsupportedOperationException(method.name)
            }
        } as JavaClass

    private fun read(text: String, known: Set<ClassId> = emptySet()): JavaModuleInfo? =
        readJavaModuleInfoSource(text) { classId -> if (classId in known) stubClass(classId) else null }

    @Test
    fun readsNameRequiresAndExports() {
        val info = requireNotNull(
            read(
                """
                module my.module {
                    requires a.b;
                    requires transitive c;
                    requires static d;
                    exports p.q;
                    exports p.r to m1, m2;
                    opens p.s;
                    opens p.t to m3;
                    uses p.q.Service;
                    provides p.q.Service with p.r.Impl, p.r.Impl2;
                }
                """.trimIndent()
            )
        )

        assertEquals("my.module", info.moduleName)
        assertEquals(
            listOf(
                JavaModuleInfo.Requires("a.b", false),
                JavaModuleInfo.Requires("c", true),
                JavaModuleInfo.Requires("d", false),
            ),
            info.requires
        )
        assertEquals(
            listOf(
                JavaModuleInfo.Exports(FqName("p.q"), emptyList()),
                JavaModuleInfo.Exports(FqName("p.r"), listOf("m1", "m2")),
            ),
            info.exports
        )
        assertEquals(emptyList<Any>(), info.annotations)
    }

    @Test
    fun readsAnnotationsOfOpenModule() {
        val info = requireNotNull(
            read(
                """
                import org.jspecify.annotations.NullMarked;
                @NullMarked
                @Deprecated
                open module foo.bar { }
                """.trimIndent(),
                known = setOf(nullMarked, deprecated),
            )
        )

        assertEquals("foo.bar", info.moduleName)
        assertEquals(2, info.annotations.size)
        val classIds = info.annotations.map { it.classId }
        assertEquals(listOf<ClassId?>(nullMarked, deprecated), classIds)
    }

    @Test
    fun readsFullyQualifiedModuleAnnotation() {
        val info = requireNotNull(
            read("@org.jspecify.annotations.NullMarked module x {}", known = setOf(nullMarked))
        )

        assertEquals("x", info.moduleName)
        assertEquals(listOf<ClassId?>(nullMarked), info.annotations.map { it.classId })
    }

    @Test
    fun returnsNullWithoutModuleDeclaration() {
        assertNull(read("package p; class A {}"))
    }

    @Test
    fun toleratesWhitespaceAroundDots() {
        val info = requireNotNull(read("module a . b { requires c . d; exports e . f to g . h; }"))

        assertEquals("a.b", info.moduleName)
        assertEquals(listOf(JavaModuleInfo.Requires("c.d", false)), info.requires)
        assertEquals(listOf(JavaModuleInfo.Exports(FqName("e.f"), listOf("g.h"))), info.exports)
    }
}
