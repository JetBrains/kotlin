/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.imports

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImport
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImportPriority
import org.jetbrains.kotlin.resolve.ImportPath

@KaImplementationDetail
class KaDefaultImportImpl(
    override val importPath: ImportPath,
    override val priority: KaDefaultImportPriority,
) : KaDefaultImport {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KaDefaultImport
                && other.importPath == importPath
                && other.priority == priority
    }

    override fun hashCode(): Int {
        var result = importPath.hashCode()
        result = 31 * result + priority.hashCode()
        return result
    }
}
