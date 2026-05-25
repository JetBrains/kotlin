/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.imports

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Represents an `import` directive declared in a [org.jetbrains.kotlin.psi.KtFile].
 *
 * [KaImport] is the common supertype of:
 *
 * - [KaExplicitImport] — a non-star import (`import a.b.c` or `import a.b.c as alias`).
 * - [KaStarImport] — a star import (`import a.b.*`).
 *
 * Default imports (those added implicitly by the platform, e.g., `kotlin.*`) are not represented by [KaImport].
 * They are exposed via [KaDefaultImportsProvider].
 */
@KaExperimentalApi
public sealed interface KaImport : KaLifetimeOwner {
    /**
     * The fully-qualified name that appears in the import directive.
     *
     * For [KaExplicitImport], this is the FqName of the imported declaration (e.g., `kotlin.collections.listOf`).
     * For [KaStarImport], this is the FqName of the imported package or class (e.g., `java.util`).
     *
     * `null` for unresolvable imports whose path could not be parsed.
     */
    public val importedFqName: FqName?

    /**
     * The source PSI of the import directive, or `null` if this import has no corresponding PSI element.
     */
    public val psi: KtImportDirective?
}

/**
 * Represents a non-star `import` directive, with or without an `as` alias.
 *
 * #### Examples
 *
 * ```kotlin
 * import java.util.Date              // aliasName = null, importedName = Name("Date")
 * import java.util.Date as JavaDate  // aliasName = Name("JavaDate"), importedName = Name("JavaDate")
 * import kotlin.collections.listOf   // resolves to multiple overloaded callable symbols
 * ```
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaExplicitImport : KaImport {
    /**
     * The classifier referenced by this import, or `null` if [importedFqName] does not name a classifier
     * resolvable from the use-site module.
     *
     * A single import directive may resolve to both a classifier and a set of callables simultaneously
     * (e.g., `import a.b.c` where `c` is both a class and a top-level function); the classifier is reported
     * here, while callables are reported in [callableSymbols].
     */
    public val classifierSymbol: KaClassifierSymbol?

    /**
     * The callables referenced by this import. May contain multiple symbols for overloaded callables
     * (e.g., `import kotlin.collections.listOf` resolves to every `listOf` overload). Empty if [importedFqName]
     * does not name any callable resolvable from the use-site module.
     *
     * The list is ordered, but the order is not specified.
     */
    public val callableSymbols: List<KaCallableSymbol>

    /**
     * The alias from `import X as Y`, or `null` if this import has no `as` clause.
     */
    public val aliasName: Name?

    /**
     * The short name under which the imported declarations are visible in the file:
     * [aliasName] if present, otherwise [importedFqName]`.shortName()`.
     *
     * `null` if [importedFqName] is `null` and [aliasName] is `null`.
     */
    public val importedName: Name?
}

/**
 * Represents a star `import` directive (`import a.b.*`).
 *
 * Star imports never carry an alias, per the Kotlin language specification.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaStarImport : KaImport {
    /**
     * The classifier whose members are star-imported, or `null` if this import targets a package
     * (or its target is unresolvable). For example, `import Foo.*` resolves to the classifier `Foo`,
     * while `import java.util.*` resolves to `null`.
     */
    public val classifierSymbol: KaClassifierSymbol?
}
