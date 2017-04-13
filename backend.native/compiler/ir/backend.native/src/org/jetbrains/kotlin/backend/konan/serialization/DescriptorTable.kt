/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import kotlin.properties.Delegates
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.backend.konan.llvm.typeInfoSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.symbolName
import org.jetbrains.kotlin.backend.konan.llvm.localHash

internal fun DeclarationDescriptor.symbolName(): String = when (this) {
    is FunctionDescriptor 
        -> this.symbolName
    is PropertyDescriptor 
        -> this.symbolName
    is ClassDescriptor 
        -> this.typeInfoSymbolName
    else -> error("Unexpected exported descriptor: $this") 
}

internal val DeclarationDescriptor.uniqId 
    get() = this.symbolName().localHash.value


// TODO: We don't manage id clashes anyhow now.
class DescriptorTable(val builtIns: IrBuiltIns) {

    val table = mutableMapOf<DeclarationDescriptor, Long>()
    var currentIndex = 0L

    init {
        builtIns.irBuiltInDescriptors.forEach { 
            table.put(it, it.uniqId)
        }
    }

    fun indexByValue(value: DeclarationDescriptor): Long {
        val index = table.getOrPut(value) { 
            if (!value.isExported()
                || value is TypeParameterDescriptor) {
                currentIndex++
            } else {
                value.uniqId
            }
        }
        return index
    }
}

class IrDeserializationDescriptorIndex(irBuiltIns: IrBuiltIns) {

    val map = mutableMapOf<Long, DeclarationDescriptor>()

    init {
        irBuiltIns.irBuiltInDescriptors.forEach { 
            map.put(it.uniqId, it)
        }
    }

}

val IrBuiltIns.irBuiltInDescriptors
    get() = listOf<FunctionDescriptor>(
        this.eqeqeq,
        this.eqeq,
        this.lt0,
        this.lteq0,
        this.gt0,
        this.gteq0,
        this.throwNpe,
        this.booleanNot,
        this.noWhenBranchMatchedException,
        this.enumValueOf)


