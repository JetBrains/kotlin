/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

interface DefaultImplsClassDescriptor : ClassDescriptor {
    val correspondingInterface: ClassDescriptor
}

class DefaultImplsClassDescriptorImpl(
        name: Name,
        override val correspondingInterface: ClassDescriptor,
        sourceElement: SourceElement
) : DefaultImplsClassDescriptor, KnownClassDescriptor(name, correspondingInterface, sourceElement, ClassKind.CLASS, Modality.FINAL, Visibilities.PUBLIC, Annotations.EMPTY) {
    init {
        initialize(emptyList(), listOf(correspondingInterface.module.builtIns.anyType))
    }

    override fun isExternal() = false
}