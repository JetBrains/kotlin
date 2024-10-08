/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.imports

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImport
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImports
import org.jetbrains.kotlin.resolve.ImportPath


@KaImplementationDetail
class KaDefaultImportsImpl(
    override val defaultImports: List<KaDefaultImport>,
    override val excludedFromDefaultImports: List<ImportPath>
) : KaDefaultImports {
    override fun equals(other: Any?): Boolean {
        return this === other
                || other is KaDefaultImports
                && other.defaultImports == defaultImports
                && other.excludedFromDefaultImports == excludedFromDefaultImports
    }

    override fun hashCode(): Int {
        var result = defaultImports.hashCode()
        result = 31 * result + excludedFromDefaultImports.hashCode()
        return result
    }
}