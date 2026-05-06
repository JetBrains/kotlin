/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.name.FqName

/**
 * Per-compilation-unit immutable data shared across all scope variants of a [JavaResolutionContext].
 *
 * Holds the per-unit [LazySessionAccess] (Step 4.5a deliverable per
 * [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §12 Q2 / §11) and the per-unit
 * [JavaSupertypeLoopChecker] (§6.1) so that all scope variants of the same compilation
 * unit share the same cycle bound.
 */
internal class CompilationUnitContext(
    val packageFqName: FqName,
    val simpleImports: Map<String, FqName>,
    val starImports: List<FqName>,
    val inheritedMemberResolver: JavaInheritedMemberResolver,
    val classFinder: LeanJavaClassFinder?,
    val lazySessionAccess: LazySessionAccess?,
    val loopChecker: JavaSupertypeLoopChecker = JavaSupertypeLoopChecker(),
)
