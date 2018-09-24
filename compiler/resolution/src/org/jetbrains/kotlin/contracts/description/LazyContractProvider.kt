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

package org.jetbrains.kotlin.contracts.description

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.serialization.deserialization.ContractProvider

/**
 * Essentially, this is a composition of two fields: value of type 'ContractDescription' and
 * 'computation', which guarantees to initialize this field.
 */
class LazyContractProvider(private val computation: () -> Any?) : ContractProvider {
    @Volatile
    private var isComputed: Boolean = false

    private var contractDescription: ContractDescription? = null


    fun getContractDescription(): ContractDescription? {
        if (!isComputed) {
            computation.invoke() // should initialize contractDescription
            assert(isComputed) { "Computation of contract hasn't initialized contract properly" }
        }

        return contractDescription
    }

    fun setContractDescription(contractDescription: ContractDescription?) {
        this.contractDescription = contractDescription
        isComputed = true // publish
    }

    companion object {
        fun createInitialized(contract: ContractDescription?): LazyContractProvider =
            LazyContractProvider({}).apply { setContractDescription(contract) }
    }
}

// For storing into UserDataMap of FunctionDescriptor
object ContractProviderKey : CallableDescriptor.UserDataKey<LazyContractProvider?>
