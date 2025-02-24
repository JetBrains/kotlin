/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.FirTypeDeserializer
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.metadata.ProtoBuf

class JsFlexibleTypeFactory(private val session: FirSession) : FirTypeDeserializer.FlexibleTypeFactory {
    override fun createFlexibleType(proto: ProtoBuf.Type, lowerBound: ConeRigidType, upperBound: ConeRigidType): ConeFlexibleType {
        return ConeFlexibleType(lowerBound, upperBound, isTrivial = false)
    }

    override fun createDynamicType(proto: ProtoBuf.Type, lowerBound: ConeRigidType, upperBound: ConeRigidType): ConeDynamicType {
        return ConeDynamicType.create(session, lowerBound.attributes)
    }
}
