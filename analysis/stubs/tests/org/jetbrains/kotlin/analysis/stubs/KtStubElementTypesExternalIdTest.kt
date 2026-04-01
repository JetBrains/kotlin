/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.stubs.ObjectStubSerializer
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KtStubElementTypesExternalIdTest {
    @Test
    fun testExternalIds() {
        val clazz = KtStubElementTypes::class.java
        for (declaredField in clazz.declaredFields) {
            val stubSerializer = declaredField.get(null) as? ObjectStubSerializer<*, *> ?: continue
            val name = declaredField.name
            val externalId = stubSerializer.externalId
            assertEquals("kotlin.$name", externalId)
        }
    }
}
