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

import org.jetbrains.kotlin.codegen.descriptors.FileClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class FileClassDescriptorImpl(
        name: Name,
        containingDeclaration: PackageFragmentDescriptor,
        supertypes: List<KotlinType>,
        sourceElement: SourceElement,
        annotations: Annotations
) : FileClassDescriptor, KnownClassDescriptor(
        name, containingDeclaration, sourceElement,
        ClassKind.CLASS, Modality.FINAL, Visibilities.PUBLIC,
        annotations
) {
    init {
        initialize(emptyList(), supertypes)
    }

    override fun isExternal() = false
}
