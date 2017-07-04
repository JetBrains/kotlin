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

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType

interface AnnotationDescriptor {
    val type: KotlinType

    val fqName: FqName? get() =
            (type.constructor.declarationDescriptor as? ClassDescriptor)?.fqNameUnsafe?.takeIf(FqNameUnsafe::isSafe)?.toSafe()

    @Deprecated("Use allValueArguments instead")
    val valueArgumentsByParameterDescriptor: Map<ValueParameterDescriptor, ConstantValue<*>>

    val allValueArguments: Map<Name, ConstantValue<*>>
        get() = valueArgumentsByParameterDescriptor.mapKeys { (parameter) -> parameter.name }

    val source: SourceElement
}
