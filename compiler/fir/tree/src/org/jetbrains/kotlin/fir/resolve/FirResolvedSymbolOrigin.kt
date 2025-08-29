/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

enum class FirResolvedSymbolOrigin {
    /**
     * Resolved as one of the default imports on every Kotlin file.
     */
    DefaultImport,

    /**
     * Resolved through an import of the form `import some.package.*`.
     */
    StarImport,

    /**
     * Resolved through an import of the form `import some.package.Thing`.
     */
    ExplicitImport,

    /**
     * Resolved in the same package as the current declaration.
     */
    Package,

    /**
     * Resolved through a qualified name `some.package.Thing`.
     */
    Qualified,

    /**
     * Resolved through context-sensitive resolution.
     */
    ContextSensitive
}