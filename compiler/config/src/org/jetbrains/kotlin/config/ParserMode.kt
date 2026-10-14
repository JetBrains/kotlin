/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class ParserMode(val treeBased: Boolean) {
    Psi(treeBased = false),
    LightTree(treeBased = true),
    KmpTree(treeBased = true);

    companion object {
        // TODO: change to LightTree (KT-89663)
        // Immediate change causes a bunch of failures in
        // JsInvalidationPerFileTestGenerated, KaptStubConverterJTreeTestGenerated
        // and some more tests
        val Default = Psi
    }
}
