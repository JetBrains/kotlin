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
import org.jetbrains.kotlin.descriptors.ContractProvider
import org.jetbrains.kotlin.storage.StorageManager

abstract class AbstractContractProvider : ContractProvider {
    abstract fun getContractDescription(): ContractDescription?
}

/**
 * Essentially, this is a composition of two fields: value of type 'ContractDescription' and
 * 'computation', which guarantees to initialize this field.
 *
 * Such contract providers are present only for source-based declarations, where we need additional
 * resolve (force-resolve of the body) to get ContractDescription
 */
class LazyContractProvider(private val storageManager: StorageManager, private val computation: () -> Any?) : AbstractContractProvider() {
    @Volatile
    private var isComputed: Boolean = false

    private var contractDescription: ContractDescription? = null


    override fun getContractDescription(): ContractDescription? {
        if (!isComputed) {
            storageManager.compute(computation)
            assert(isComputed) { "Computation of contract hasn't initialized contract properly" }
        }

        return contractDescription
    }

    fun setContractDescription(contractDescription: ContractDescription?) {
        this.contractDescription = contractDescription
        isComputed = true // publish
    }
}

/**
 * Such contract providers are used where we can be sure about contract presence and don't need
 * additional resolve (e.g., for deserialized declarations)
 */
class ContractProviderImpl(private val contractDescription: ContractDescription) : AbstractContractProvider() {
    override fun getContractDescription(): ContractDescription = contractDescription
}

// For storing into UserDataMap of FunctionDescriptor
object ContractProviderKey : CallableDescriptor.UserDataKey<AbstractContractProvider?>
