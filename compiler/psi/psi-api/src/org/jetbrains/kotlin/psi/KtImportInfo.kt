/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * A representation-agnostic view of a single import statement.
 *
 * The same information is available whether the import is backed by a full [KtImportDirective] PSI element or by a
 * lightweight stub, so consumers that only need the imported name, alias, or all-under flag can work uniformly against
 * either representation.
 *
 * ### Example:
 *
 * ```kotlin
 * import kotlin.collections.List as KList  // importedFqName = kotlin.collections.List, aliasName = "KList"
 * import kotlin.collections.*              // isAllUnder = true
 * ```
 */
interface KtImportInfo {
    /**
     * The imported reference, either as a resolved fully qualified name or as the raw reference expression.
     */
    sealed class ImportContent {
        /** The import is described by a reference [expression] (the PSI-backed form). */
        class ExpressionBased(val expression: KtExpression) : ImportContent()

        /** The import is described directly by a [fqName] (the stub-backed form). */
        class FqNameBased(val fqName: FqName) : ImportContent()
    }

    /** `true` for an all-under import (`import foo.*`), which imports all members of the given scope. */
    val isAllUnder: Boolean

    /** The imported reference, or `null` if it is missing in incomplete code. */
    val importContent: ImportContent?

    /** The fully qualified name being imported, or `null` if it cannot be determined. */
    val importedFqName: FqName?

    /** The alias assigned with `as`, or `null` if the import has no alias. */
    val aliasName: String?

    /**
     * The name under which the imported declaration becomes visible: the [alias][aliasName] if present, otherwise the
     * short name of the imported reference. `null` for an all-under import or when the name cannot be determined.
     */
    val importedName: Name?
        get() {
            return computeNameAsString()?.takeIf(CharSequence::isNotEmpty)?.let(Name::identifier)
        }

    private fun computeNameAsString(): String? {
        if (isAllUnder) return null
        aliasName?.let { return it }
        val importContent = importContent
        return when (importContent) {
            is ImportContent.ExpressionBased -> KtPsiUtil.getLastReference(importContent.expression)?.getReferencedName()
            is ImportContent.FqNameBased -> importContent.fqName.takeUnless(FqName::isRoot)?.shortName()?.asString()
            null -> null
        }
    }
}
