/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.name.FqName

/** Per-file immutable data shared across all scope variants of a [JavaResolutionContext]. */
internal class JavaFileContext(
    val packageFqName: FqName,
    val imports: JavaImports,
    val classFinder: LeanJavaClassFinder?,
    val session: FirSession,
)
