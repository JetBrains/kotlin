/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.name.FqName

/**
 * Per-file immutable data shared across all scope variants of a [JavaResolutionContext].
 *
 * [imports] is the four-bucket [JavaImports] holder produced by
 * [JavaImportResolver.extractImports]; see its KDoc for the JLS 7.5 / 6.4.1 semantics of each
 * bucket.
 */
internal class JavaFileContext(
    val packageFqName: FqName,
    val imports: JavaImports,
    val inheritedMemberResolver: JavaInheritedMemberResolver,
    val classFinder: LeanJavaClassFinder?,
    val session: FirSession,
)
