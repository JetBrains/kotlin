/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.QualifiedNameTable.QualifiedName
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class NameResolverImplTest {
    @Test
    fun rejectsSelfParentCycle() {
        val resolver = resolver(qualifiedName(0, CLASS, parent = 0))

        assertCycleRejected(resolver)
    }

    @Test
    fun rejectsTwoEntryCycle() {
        val resolver = resolver(
            qualifiedName(0, CLASS, parent = 1),
            qualifiedName(1, CLASS, parent = 0),
        )

        assertCycleRejected(resolver)
    }

    @Test
    fun resolvesChainUsingEveryTableEntry() {
        val resolver = resolver(
            qualifiedName(0, PACKAGE),
            qualifiedName(1, PACKAGE, parent = 0),
            qualifiedName(2, CLASS, parent = 1),
        )

        assertEquals("name0/name1/name2", resolver.getQualifiedClassName(2))
    }

    private fun assertCycleRejected(resolver: NameResolverImpl) {
        assertTimeoutPreemptively(Duration.ofSeconds(5)) {
            assertThrows(IllegalStateException::class.java) { resolver.getQualifiedClassName(0) }
        }
    }

    private fun resolver(vararg qualifiedNames: QualifiedName): NameResolverImpl {
        val strings = ProtoBuf.StringTable.newBuilder().apply {
            qualifiedNames.indices.forEach { addString("name$it") }
        }.build()
        val names = ProtoBuf.QualifiedNameTable.newBuilder().apply {
            qualifiedNames.forEach(::addQualifiedName)
        }.build()
        return NameResolverImpl(strings, names)
    }

    private fun qualifiedName(shortName: Int, kind: QualifiedName.Kind, parent: Int = -1): QualifiedName =
        QualifiedName.newBuilder()
            .setShortName(shortName)
            .setKind(kind)
            .setParentQualifiedName(parent)
            .build()
}
