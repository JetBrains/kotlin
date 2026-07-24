/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Returns a [KaPackageSymbol] corresponding to the given [fqName] if that package exists and is visible from the current use site, or
 * `null` otherwise.
 */
context(session: KaSession)
public fun findPackage(fqName: FqName): KaPackageSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolProvider.findPackage(fqName)
}

/**
 * Returns a [KaNamedClassSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
 */
context(session: KaSession)
public fun findClass(classId: ClassId): KaNamedClassSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolProvider.findClass(classId)
}

/**
 * Returns a [KaTypeAliasSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
 */
context(session: KaSession)
public fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolProvider.findTypeAlias(classId)
}

/**
 * Returns a [KaClassLikeSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
 *
 * The function combines both class search (see [findClass]) and type alias search (see [findTypeAlias]).
 */
context(session: KaSession)
public fun findClassLike(classId: ClassId): KaClassLikeSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolProvider.findClassLike(classId)
}

/**
 * Finds top-level functions and properties called [name] in the package called [packageFqName]. Returns only symbols that are visible
 * from the current use-site module.
 */
context(session: KaSession)
public fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolProvider.findTopLevelCallables(packageFqName, name)
}

/**
 * A [KaPackageSymbol] for the *root package*, which is the special package with an empty fully-qualified name.
 */
context(session: KaSession)
public val rootPackageSymbol: KaPackageSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.rootPackageSymbol
    }
