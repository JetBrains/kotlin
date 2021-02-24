/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.scopes

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface KtImportingScope : KtScope {
    val imports: List<Import>
    val isDefaultImportingScope: Boolean
}

interface KtStarImportingScope : KtImportingScope {
    override val imports: List<StarImport>
}

interface KtNonStarImportingScope : KtImportingScope {
    override val imports: List<NonStarImport>
}

sealed class Import {
    abstract val packageFqName: FqName
    abstract val relativeClassName: FqName?
    abstract val resolvedClassId: ClassId?
}

class NonStarImport(
    override val packageFqName: FqName,
    override val relativeClassName: FqName?,
    override val resolvedClassId: ClassId?,
    val callableName: Name?,
) : Import()

class StarImport(
    override val packageFqName: FqName,
    override val relativeClassName: FqName?,
    override val resolvedClassId: ClassId?,
) : Import()