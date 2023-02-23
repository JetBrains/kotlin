/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

interface IrInternationService {
    fun string(string: String): String {
        return string
    }

    fun name(string: String): Name {
        return Name.guessByFirstCharacter(string)
    }

    fun simpleType(type: IrSimpleType): IrSimpleType {
        return type
    }

    fun clear() {

    }
}

class DefaultIrInternationService : IrInternationService {
    private val strings = hashMapOf<String, String>()
    private val names = hashMapOf<String, Name>()
    private val simpleTypes = hashMapOf<Pair<IdSignature, SimpleTypeNullability>, IrSimpleType>()

    override fun string(string: String): String {
        return strings.getOrPut(string) { string }
    }

    override fun name(string: String): Name {
        return names.getOrPut(string) { super.name(string) }
    }

    override fun simpleType(type: IrSimpleType): IrSimpleType {
        val signature = type.classifier.signature
        if (
            signature != null &&
            type.arguments.isEmpty() &&
            type.annotations.isEmpty() && type.abbreviation == null
        ) {
            return simpleTypes.getOrPut(signature to type.nullability) { type }
        }
        return super.simpleType(type)
    }

    override fun clear() {
        strings.clear()
        names.clear()
        simpleTypes.clear()
    }
}