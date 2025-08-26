/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtTypeAlias

/**
 * The [KotlinStandaloneDeclarationIndex] that is lazily built from a [lazyIndex] on demand.
 *
 * @see KotlinStandaloneDeclarationIndexImpl
 */
internal class KotlinStandaloneLazyDeclarationIndexImpl(
    private val lazyIndex: Lazy<KotlinStandaloneDeclarationIndex>
) : KotlinStandaloneDeclarationIndex {
    private val computedIndex: KotlinStandaloneDeclarationIndex get() = lazyIndex.value

    override val facadeFileMap: Map<FqName, Set<KtFile>> get() = computedIndex.facadeFileMap
    override val multiFileClassPartMap: Map<FqName, Set<KtFile>> get() = computedIndex.multiFileClassPartMap
    override val scriptMap: Map<FqName, Set<KtScript>> get() = computedIndex.scriptMap
    override val classMap: Map<FqName, Set<KtClassOrObject>> get() = computedIndex.classMap
    override val typeAliasMap: Map<FqName, Set<KtTypeAlias>> get() = computedIndex.typeAliasMap
    override val topLevelFunctionMap: Map<FqName, Set<KtNamedFunction>> get() = computedIndex.topLevelFunctionMap
    override val topLevelPropertyMap: Map<FqName, Set<KtProperty>> get() = computedIndex.topLevelPropertyMap
    override val classesBySupertypeName: Map<Name, Set<KtClassOrObject>> get() = computedIndex.classesBySupertypeName
    override val inheritableTypeAliasesByAliasedName: Map<Name, Set<KtTypeAlias>> get() = computedIndex.inheritableTypeAliasesByAliasedName
}
