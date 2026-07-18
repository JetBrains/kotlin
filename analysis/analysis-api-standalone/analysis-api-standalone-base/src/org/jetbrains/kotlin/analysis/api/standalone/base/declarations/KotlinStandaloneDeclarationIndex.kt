/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal interface KotlinStandaloneDeclarationIndex {
    /**
     * All indexed files keyed by their [KtFile.getPackageFqName]. In contrast to the declaration-based maps, this map also contains files
     * without any top-level declarations, so it can be used to find every package that is mentioned in a package directive.
     */
    val filesByPackage: Map<FqName, Set<KtFile>>

    val facadeFileMap: Map<FqName, Set<KtFile>>
    val multiFileClassPartMap: Map<FqName, Set<KtFile>>
    val scriptMap: Map<FqName, Set<KtScript>>

    val classesByClassId: Map<ClassId, Set<KtClassOrObject>>
    val typeAliasesByClassId: Map<ClassId, Set<KtTypeAlias>>
    val topLevelFunctionsByCallableId: Map<CallableId, Set<KtNamedFunction>>
    val topLevelPropertiesByCallableId: Map<CallableId, Set<KtProperty>>

    val classLikeDeclarationsByPackage: Map<FqName, Set<KtClassLikeDeclaration>>
    val topLevelCallablesByPackage: Map<FqName, Set<KtCallableDeclaration>>

    /**
     * Allows quickly finding [KtClassOrObject]s which have a given simple name as a supertype. The map may contain local classes as well.
     */
    val classesBySupertypeName: Map<Name, Set<KtClassOrObject>>

    /**
     * Maps a simple name `N` to type aliases `A` in whose definition `N` occurs as the topmost user type, which is a prerequisite for other
     * classes inheriting from `N` by referring to `A`. Does not support function types (e.g. `Function1`).
     *
     * There is no guarantee that the type alias can be inherited from. For example, if its expanded type is final, the type alias is not
     * inheritable. The resulting type alias `A` may also occur in the expanded type of another type alias (which may also be inheritable),
     * so the index may need to be followed transitively.
     *
     * The index is used to find direct class inheritors.
     *
     * ### Example
     *
     * ```
     * abstract class C
     *
     * typealias A = C
     *
     * class X : A()
     * ```
     *
     * The index contains the following entry: `"C" -> A`.
     */
    val inheritableTypeAliasesByAliasedName: Map<Name, Set<KtTypeAlias>>
}
