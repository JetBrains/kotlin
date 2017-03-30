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


// TODO: we currently just assign each encountered
// descriptor a new int id. 
// That;s okay until we have more than one external module.
// In that case the descriptors will get different ids.
// Need to come up with a stable scheme of ids 
// dependant on something like FunctionDescriptor.functionName
class DescriptorTable(val builtIns: IrBuiltIns) {

    val table = mutableMapOf<DeclarationDescriptor, Int>()
    var currentIndex = 0

    init {
        builtIns.irBuiltInDescriptors.forEachIndexed { 
            index, descriptor ->

            table.put(descriptor, index)
            currentIndex = index + 1
        }
    }

    fun indexByValue(value: DeclarationDescriptor): Int {
        val index = table.getOrPut(value) { 
            currentIndex ++ 
        }
        return index
    }
}

class IrDeserializationDescriptorIndex(irBuiltIns: IrBuiltIns) {

    val map = mutableMapOf<Int, DeclarationDescriptor>()

    init {
        irBuiltIns.irBuiltInDescriptors.forEachIndexed { 
            index, descriptor ->
                map.put(index, descriptor)
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


