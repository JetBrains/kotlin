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

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.org.objectweb.asm.Type

interface ReferenceValueDescriptor {
    fun onUseAsTainted()
}

sealed class TrackedReferenceValue(type: Type): StrictBasicValue(type) {
    abstract val descriptors: Set<ReferenceValueDescriptor>
}

class ProperTrackedReferenceValue(type: Type, val descriptor: ReferenceValueDescriptor) : TrackedReferenceValue(type) {
    override val descriptors: Set<ReferenceValueDescriptor>
        get() = setOf(descriptor)

    override fun equals(other: Any?): Boolean =
            other === this ||
            other is ProperTrackedReferenceValue && other.descriptor == this.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            "[$descriptor]"
}


class MergedTrackedReferenceValue(type: Type, override val descriptors: Set<ReferenceValueDescriptor>) : TrackedReferenceValue(type) {
    override fun equals(other: Any?): Boolean =
            other === this ||
            other is MergedTrackedReferenceValue && other.descriptors == this.descriptors

    override fun hashCode(): Int =
            descriptors.hashCode()

    override fun toString(): String =
            descriptors.toString()
}


class TaintedTrackedReferenceValue(type: Type, override val descriptors: Set<ReferenceValueDescriptor>) : TrackedReferenceValue(type) {
    override fun equals(other: Any?): Boolean =
            other === this ||
            other is TaintedTrackedReferenceValue && other.descriptors == this.descriptors

    override fun hashCode(): Int =
            descriptors.hashCode()
    override fun toString(): String =
            "!$descriptors"
}
