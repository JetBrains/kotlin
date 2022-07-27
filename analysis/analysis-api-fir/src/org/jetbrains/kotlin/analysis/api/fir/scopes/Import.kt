/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal sealed class Import {
    abstract val packageFqName: FqName
    abstract val relativeClassName: FqName?
    abstract val resolvedClassId: ClassId?
}

internal class NonStarImport(
    override val packageFqName: FqName,
    override val relativeClassName: FqName?,
    override val resolvedClassId: ClassId?,
    val callableName: Name?,
    val aliasName: Name?,
) : Import()

internal class StarImport(
    override val packageFqName: FqName,
    override val relativeClassName: FqName?,
    override val resolvedClassId: ClassId?,
) : Import()