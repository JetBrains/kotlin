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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.TypeResolver.TypeTransformerForTests
import org.jetbrains.kotlin.types.*

object InternalFlexibleTypeTransformer : TypeTransformerForTests() {
    // This is a "magic" classifier: when type resolver sees it in the code, e.g. ft<Foo, Foo?>, instead of creating a normal type,
    // it creates a flexible type, e.g. (Foo..Foo?).
    // This is used in tests and Evaluate Expression to have flexible types in the code,
    // but normal users should not be referencing this classifier
    @JvmField
    val FLEXIBLE_TYPE_CLASSIFIER: ClassId = ClassId.topLevel(FqName("kotlin.internal.flexible.ft"))

    override fun transformType(kotlinType: KotlinType): KotlinType? {
        val descriptor = kotlinType.constructor.declarationDescriptor
        if (descriptor != null && FLEXIBLE_TYPE_CLASSIFIER.asSingleFqName().toUnsafe() == DescriptorUtils.getFqName(descriptor)
            && kotlinType.arguments.size == 2) {
            return KotlinTypeFactory.flexibleType(kotlinType.arguments[0].type.unwrap() as SimpleType, kotlinType.arguments[1].type.unwrap() as SimpleType)
        }
        return null
    }
}
