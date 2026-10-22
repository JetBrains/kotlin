/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.declarations

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * The [KotlinStandaloneDeclarationIndex] that is lazily built from a [lazyIndex] on demand.
 *
 * @see KotlinStandaloneDeclarationIndexImpl
 */
internal class KotlinStandaloneLazyDeclarationIndexImpl(
    private val lazyIndex: Lazy<KotlinStandaloneDeclarationIndex>
) : KotlinStandaloneDeclarationIndex {
    private val computedIndex: KotlinStandaloneDeclarationIndex get() = lazyIndex.value

    override val facadeFileMap: Map<FqName, Set<KotlinStandaloneIndexEntry<KtFile>>> get() = computedIndex.facadeFileMap
    override val multiFileClassPartMap: Map<FqName, Set<KotlinStandaloneIndexEntry<KtFile>>> get() = computedIndex.multiFileClassPartMap
    override val scriptMap: Map<FqName, Set<KotlinStandaloneIndexEntry<KtScript>>> get() = computedIndex.scriptMap

    override val classesByClassId: Map<ClassId, Set<KotlinStandaloneIndexEntry<KtClassOrObject>>> get() = computedIndex.classesByClassId
    override val typeAliasesByClassId: Map<ClassId, Set<KotlinStandaloneIndexEntry<KtTypeAlias>>> get() = computedIndex.typeAliasesByClassId
    override val topLevelFunctionsByCallableId: Map<CallableId, Set<KotlinStandaloneIndexEntry<KtNamedFunction>>>
        get() = computedIndex.topLevelFunctionsByCallableId
    override val topLevelPropertiesByCallableId: Map<CallableId, Set<KotlinStandaloneIndexEntry<KtProperty>>>
        get() = computedIndex.topLevelPropertiesByCallableId

    override val classLikeDeclarationsByPackage: Map<FqName, Set<KotlinStandaloneIndexEntry<KtClassLikeDeclaration>>>
        get() = computedIndex.classLikeDeclarationsByPackage

    override val topLevelCallablesByPackage: Map<FqName, Set<KotlinStandaloneIndexEntry<KtCallableDeclaration>>>
        get() = computedIndex.topLevelCallablesByPackage

    override val classesBySupertypeName: Map<Name, Set<KotlinStandaloneIndexEntry<KtClassOrObject>>>
        get() = computedIndex.classesBySupertypeName
    override val inheritableTypeAliasesByAliasedName: Map<Name, Set<KotlinStandaloneIndexEntry<KtTypeAlias>>>
        get() = computedIndex.inheritableTypeAliasesByAliasedName
}
