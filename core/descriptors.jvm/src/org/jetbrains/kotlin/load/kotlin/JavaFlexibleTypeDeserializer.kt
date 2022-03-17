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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.load.java.lazy.types.RawTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind

object JavaFlexibleTypeDeserializer : FlexibleTypeDeserializer {
    override fun create(proto: ProtoBuf.Type, flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (flexibleId != JvmProtoBufUtil.PLATFORM_TYPE_ID) {
            return ErrorUtils.createErrorType(ErrorTypeKind.ERROR_FLEXIBLE_TYPE, flexibleId, lowerBound.toString(), upperBound.toString())
        }
        if (proto.hasExtension(JvmProtoBuf.isRaw)) {
            return RawTypeImpl(lowerBound, upperBound)
        }
        return KotlinTypeFactory.flexibleType(lowerBound, upperBound)
    }
}
