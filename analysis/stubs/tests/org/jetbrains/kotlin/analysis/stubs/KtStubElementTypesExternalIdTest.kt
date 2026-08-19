/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KtStubElementTypesExternalIdTest {
    @Test
    @OptIn(KtImplementationDetail::class)
    fun testExternalIds() {
        for (declaredField in KtStubElementTypes::class.java.declaredFields) {
            val elementType = declaredField.get(null) as KtStubElementType<*, *>
            val fieldName = declaredField.name

            // StubElementTypeHolderEP explicitly says that the debug name must be the same as the field
            assertEquals(fieldName, elementType.toString())

            val externalId = elementType.externalId
            assertEquals("kotlin.$fieldName", externalId)
        }
    }
}
