/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.ir.declarations.IrDeclaration

class DceDumpNameCache {
    /**
     * This cache provides better dumping names for reachability and ir sizes infos.
     * The problem is that dumps contain several same named objects that should be consistently renamed.
     * The name for the first declaration with some name isn't changed. For other names suffix (i) is added, where i >= 1
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
}