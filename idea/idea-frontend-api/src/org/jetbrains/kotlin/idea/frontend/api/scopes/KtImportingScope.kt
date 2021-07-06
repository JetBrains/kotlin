/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.scopes

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

public interface KtImportingScope : KtScope {
    public val imports: List<Import>
    public val isDefaultImportingScope: Boolean
}

public interface KtStarImportingScope : KtImportingScope {
    override val imports: List<StarImport>
}

public interface KtNonStarImportingScope : KtImportingScope {
    override val imports: List<NonStarImport>
}

public sealed class Import {
    public abstract val packageFqName: FqName
    public abstract val relativeClassName: FqName?
    public abstract val resolvedClassId: ClassId?
}

public class NonStarImport(
    public override val packageFqName: FqName,
    override val relativeClassName: FqName?,
    override val resolvedClassId: ClassId?,
    public val callableName: Name?,
) : Import()

public class StarImport(
    override val packageFqName: FqName,
    override val relativeClassName: FqName?,
    override val resolvedClassId: ClassId?,
) : Import()