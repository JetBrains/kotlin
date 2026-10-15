/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.library.impl.IrStringWriter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class IrStringSerializer {
    private val protoStringMap = hashMapOf<String, Int>()
    private val protoStringArray = arrayListOf<String>()

    fun serializeString(value: String): Int = protoStringMap.getOrPut(value) {
        protoStringArray.add(value)
        protoStringArray.size - 1
    }

    fun serializeName(name: Name): Int = serializeString(name.asString())

    fun serializeFqName(fqName: FqName): List<Int> = serializeFqName(fqName.asString())
    fun serializeFqName(fqName: String): List<Int> = fqName.split('.').map { serializeString(it) }

    fun toIrStringWriter(useVarIntInDataArrays: Boolean): IrStringWriter =
        IrStringWriter(protoStringArray, useVarIntInDataArrays)
}
