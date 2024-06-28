/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class KotlinStandaloneDeclarationIndex {
    internal val facadeFileMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    internal val multiFileClassPartMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    internal val scriptMap: MutableMap<FqName, MutableSet<KtScript>> = mutableMapOf()
    internal val classMap: MutableMap<FqName, MutableSet<KtClassOrObject>> = mutableMapOf()
    internal val typeAliasMap: MutableMap<FqName, MutableSet<KtTypeAlias>> = mutableMapOf()
    internal val topLevelFunctionMap: MutableMap<FqName, MutableSet<KtNamedFunction>> = mutableMapOf()
    internal val topLevelPropertyMap: MutableMap<FqName, MutableSet<KtProperty>> = mutableMapOf()

    /**
     * Allows quickly finding [KtClassOrObject]s which have a given simple name as a supertype. The map may contain local classes as well.
     */
    internal val classesBySupertypeName: MutableMap<Name, MutableSet<KtClassOrObject>> = mutableMapOf()

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
    internal val inheritableTypeAliasesByAliasedName: MutableMap<Name, MutableSet<KtTypeAlias>> = mutableMapOf()
}
