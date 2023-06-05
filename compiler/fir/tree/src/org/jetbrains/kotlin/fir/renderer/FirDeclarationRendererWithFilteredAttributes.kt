/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

class FirDeclarationRendererWithFilteredAttributes : FirDeclarationRendererWithAttributes() {
    override fun attributeTypesToIds(): List<Pair<String, Int>> {
        return super.attributeTypesToIds().filter { it.first !in IGNORED_ATTRIBUTES }
    }

    private companion object {
        private val IGNORED_ATTRIBUTES = setOf("FirVersionRequirementsTableKey", "SourceElementKey")
    }
}