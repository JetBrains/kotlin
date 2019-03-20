/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.checker.StrictEqualityTypeChecker
import org.jetbrains.kotlin.types.createDynamicType
import org.jetbrains.kotlin.types.typeUtil.builtIns

object DynamicTypeDeserializer : FlexibleTypeDeserializer {
    const val id = "kotlin.DynamicType"

    override fun create(proto: ProtoBuf.Type, flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (flexibleId != id) return ErrorUtils.createErrorType("Unexpected id: $flexibleId. ($lowerBound..$upperBound)")

        return if (StrictEqualityTypeChecker.strictEqualTypes(lowerBound, lowerBound.builtIns.nothingType) &&
            StrictEqualityTypeChecker.strictEqualTypes(upperBound, upperBound.builtIns.nullableAnyType)
        ) {
            createDynamicType(lowerBound.builtIns)
        } else {
            ErrorUtils.createErrorType("Illegal type range for dynamic type: $lowerBound..$upperBound")
        }
    }
}
