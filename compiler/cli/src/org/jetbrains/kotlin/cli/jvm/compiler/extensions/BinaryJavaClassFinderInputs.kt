/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.extensions

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex

/**
 * Raw inputs needed to construct an index-based, PSI-free binary `JavaClassFinder`.
 *
 * The CLI environment (`VfsBasedProjectEnvironment`) supplies these inputs lazily, and the
 * `JavaClassFinderFactory` extension (currently `JavaClassFinderOverAstFactory` in
 * `compiler/java-direct`) constructs the actual finder. The indirection exists purely to avoid
 * a circular module dependency: `compiler/cli` cannot reference types from `compiler/java-direct`,
 * which itself depends on `compiler/cli`.
 *
 * Phase 1 stepping stone for replacing the PSI binary half of `CombinedJavaClassFinder`. See
 * `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`.
 *
 * @property index The classpath index that backs `CliVirtualFileFinder`. Used for per-package
 *           directory traversal (`hasPackage`, `knownClassNamesInPackage`) and per-class virtual
 *           file lookup (`findClass`).
 * @property scope PSI search scope used to filter candidate `.class`/`.sig` virtual files; same
 *           scope the source-side `FirJavaFacade` was instantiated with.
 * @property enableSearchInCtSym Whether `.sig` entries (e.g. JDK `ct.sym` stubs) should be
 *           consulted in addition to plain `.class` files. Mirrors `CliVirtualFileFinder`'s flag
 *           so the binary finder agrees with the JVM-FIR pipeline on JDK class visibility.
 */
class BinaryJavaClassFinderInputs(
    val index: JvmDependenciesIndex,
    val scope: GlobalSearchScope,
    val enableSearchInCtSym: Boolean,
)
