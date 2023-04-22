/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

internal class KotlinStaticDeclarationIndex {
    internal val facadeFileMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    internal val multiFileClassPartMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    internal val scriptMap: MutableMap<FqName, MutableSet<KtScript>> = mutableMapOf()
    internal val classMap: MutableMap<FqName, MutableSet<KtClassOrObject>> = mutableMapOf()
    internal val typeAliasMap: MutableMap<FqName, MutableSet<KtTypeAlias>> = mutableMapOf()
    internal val topLevelFunctionMap: MutableMap<FqName, MutableSet<KtNamedFunction>> = mutableMapOf()
    internal val topLevelPropertyMap: MutableMap<FqName, MutableSet<KtProperty>> = mutableMapOf()
}
