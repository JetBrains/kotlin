/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.name.FqName

class Table

sealed class KlibEntry(val fqName: FqName)

class KlibFileEntry(fqName: FqName) : KlibEntry(fqName) {
    lateinit var bodyData: ByteArray
    lateinit var headerData: ByteArray

    val stringTable = Table()
    val symbolTable = Table()
    val typeTable = Table()
}

class KlibPackageEntry(fqName: FqName) : KlibEntry(fqName) {
    val childs = mutableListOf<KlibEntry>()
}

class KlibModuleEntry(val name: String) : KlibEntry(FqName.ROOT) {
    val packages = mutableListOf<KlibEntry>()

    val stringTable = Table()
    val symbolTable = Table()
    val typeTable = Table()
}