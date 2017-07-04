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

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import kotlin.LazyThreadSafetyMode.PUBLICATION

class BuiltInAnnotationDescriptor(
        private val builtIns: KotlinBuiltIns,
        override val fqName: FqName,
        override val allValueArguments: Map<Name, ConstantValue<*>>
) : AnnotationDescriptor {
    override val type: KotlinType by lazy(PUBLICATION) {
        builtIns.getBuiltInClassByFqName(fqName).defaultType
    }

    override val source: SourceElement
        get() = SourceElement.NO_SOURCE
}
