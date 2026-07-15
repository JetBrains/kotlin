/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

class FirDeclarationRendererWithFilteredAttributes : FirDeclarationRendererWithAttributes() {
    override fun attributeTypesToIds(): List<Pair<String, Int>> {
        return super.attributeTypesToIds().filter { it.first !in IGNORED_ATTRIBUTES }
    }

    private companion object {
        private val IGNORED_ATTRIBUTES: Set<String> = hashSetOf(
            "FirVersionRequirementsTableKey",
            "SourceElementKey",
            "KlibSourceFile",
            "KlibFileAnnotationsKey",

            // In the Analysis API, the root declaration reference of each FIR declaration is already automatically checked in tests, so
            // there is no need of adding it to test output. Furthermore, only the Analysis API assigns these back references, so test
            // output would differ between compiler and Analysis API tests.
            "RootDeclarationKey",
        )
    }
}
