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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.TypeTable

interface ContractDeserializer {
    fun deserializeContractFromFunction(
        proto: ProtoBuf.Function,
        ownerFunction: FunctionDescriptor,
        typeTable: TypeTable,
        typeDeserializer: TypeDeserializer
    ): Pair<CallableDescriptor.UserDataKey<*>, ContractProvider>?

    companion object {
        val DEFAULT = object : ContractDeserializer {
            override fun deserializeContractFromFunction(
                proto: ProtoBuf.Function,
                ownerFunction: FunctionDescriptor,
                typeTable: TypeTable,
                typeDeserializer: TypeDeserializer
            ): Pair<CallableDescriptor.UserDataKey<*>, Nothing>? = null
        }
    }
}

interface ContractProvider
