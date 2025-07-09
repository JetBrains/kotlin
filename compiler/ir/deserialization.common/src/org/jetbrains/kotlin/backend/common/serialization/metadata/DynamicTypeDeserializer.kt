/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DYNAMIC_TYPE_DESERIALIZER_ID
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.checker.StrictEqualityTypeChecker
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.typeUtil.builtIns

object DynamicTypeDeserializer : FlexibleTypeDeserializer {
    const val id = DYNAMIC_TYPE_DESERIALIZER_ID

    override fun create(proto: ProtoBuf.Type, flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (flexibleId != id) {
            return ErrorUtils.createErrorType(
                ErrorTypeKind.UNEXPECTED_FLEXIBLE_TYPE_ID, flexibleId, lowerBound.toString(), upperBound.toString()
            )
        }

        return if (StrictEqualityTypeChecker.strictEqualTypes(lowerBound, lowerBound.builtIns.nothingType) &&
            StrictEqualityTypeChecker.strictEqualTypes(upperBound, upperBound.builtIns.nullableAnyType)
        ) {
            createDynamicType(lowerBound.builtIns)
        } else {
            ErrorUtils.createErrorType(ErrorTypeKind.ILLEGAL_TYPE_RANGE_FOR_DYNAMIC, lowerBound.toString(), upperBound.toString())
        }
    }
}
