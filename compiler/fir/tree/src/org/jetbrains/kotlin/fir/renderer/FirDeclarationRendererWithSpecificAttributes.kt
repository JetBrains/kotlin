/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

open class FirDeclarationRendererWithSpecificAttributes(
    private val attributeNames: Set<String>,
) : FirDeclarationRendererWithAttributes() {
    override fun attributeTypesToIds(): List<Pair<String, Int>> {
        return super.attributeTypesToIds().filter { [name, _] -> name in attributeNames }
    }
}
