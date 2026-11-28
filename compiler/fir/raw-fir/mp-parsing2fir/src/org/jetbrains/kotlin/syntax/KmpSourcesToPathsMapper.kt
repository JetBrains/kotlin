/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.jetbrains.kotlin.KtSourceElement

/**
 * The mapper for the sources based on the KMP parse tree.
 */
class KmpSourcesToPathsMapper : NonPsiSourcesToPathsMapper<Any>() {
    override val KtSourceElement.fileKey: Any
        get() = TODO("KT-91322")
}
