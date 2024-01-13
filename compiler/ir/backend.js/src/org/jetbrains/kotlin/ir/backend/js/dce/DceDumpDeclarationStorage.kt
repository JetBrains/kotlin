/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.ir.declarations.IrDeclaration

class DceDumpDeclarationStorage {
    /**
     * This storage is used to store declarations and information about duplicated names.
     * This storage solves the problem of distinguishing between same-rendered declarations (e.g., extension functions for collections)
     */
    private val nameCache: MutableMap<IrDeclaration, String> = hashMapOf()
    private val indexCache: MutableMap<String, Int> = hashMapOf()

    public fun getOrPut(declaration: IrDeclaration): String = nameCache.getOrPut(declaration) {
        val fqName = declaration.fqNameForDceDump()
        val index = indexCache.getOrDefault(fqName, 0)
        indexCache[fqName] = index + 1
        if (index == 0) fqName
        else "$fqName ($index)"
    }

    public val allCachedDeclarations: Set<IrDeclaration>
        get() = nameCache.keys
}