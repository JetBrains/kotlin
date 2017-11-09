/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.context

import org.jetbrains.kotlin.codegen.AccessorForCallableDescriptor
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

class DefaultImplsClassContext(
        typeMapper: KotlinTypeMapper,
        contextDescriptor: ClassDescriptor,
        contextKind: OwnerKind,
        parentContext: CodegenContext<*>?,
        localLookup: ((DeclarationDescriptor) -> Boolean)?,
        private val interfaceContext: ClassContext
) : ClassContext(typeMapper, contextDescriptor, contextKind, parentContext, localLookup) {

    override fun getCompanionObjectContext(): CodegenContext<*>? = interfaceContext.companionObjectContext

    override fun getAccessors(): Collection<AccessorForCallableDescriptor<*>> {
        val accessors = super.getAccessors()
        val alreadyExistKeys = accessors.map ({ Pair(it.calleeDescriptor, it.superCallTarget) })
        val filtered = interfaceContext.accessors.associateByTo(linkedMapOf()) { Pair(it.calleeDescriptor, it.superCallTarget) }.apply { keys -= alreadyExistKeys }
        return accessors + filtered.values
    }
}