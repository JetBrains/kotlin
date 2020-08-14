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

import org.jetbrains.kotlin.contracts.interpretation.ContractInterpretationDispatcher
import org.jetbrains.kotlin.contracts.model.Functor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.StorageManager

/**
 * This is actual model of contracts, i.e. what is expected to be parsed from
 * general protobuf format.
 *
 * Its intention is to provide declarative representation which is more stable
 * than inner representation of effect system, while enforcing type-checking which
 * isn't possible in protobuf representation.
 *
 * Any changes to this model should be done with previous versions in mind to keep
 * backward compatibility. Ideally, this model should only be extended, but not
 * changed.
 */
open class ContractDescription(
    val effects: List<EffectDeclaration>,
    val ownerFunction: FunctionDescriptor,
    storageManager: StorageManager
) {
    private val computeFunctor = storageManager.createNullableLazyValue {
        ContractInterpretationDispatcher().convertContractDescriptorToFunctor(this)
    }

    @Suppress("UNUSED_PARAMETER")
    fun getFunctor(usageModule: ModuleDescriptor): Functor? = computeFunctor.invoke()
}

interface ContractDescriptionElement {
    fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R
}

interface EffectDeclaration : ContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitEffectDeclaration(this, data)
}

interface BooleanExpression : ContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanExpression(this, data)
}
