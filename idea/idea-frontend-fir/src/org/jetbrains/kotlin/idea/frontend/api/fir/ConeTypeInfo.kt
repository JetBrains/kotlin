/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.idea.frontend.api.ClassTypeExpectedException
import org.jetbrains.kotlin.idea.frontend.api.ErrorTypeClassIdAccessException
import org.jetbrains.kotlin.idea.frontend.api.Invalidatable
import org.jetbrains.kotlin.idea.frontend.api.TypeInfo
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import java.lang.ref.WeakReference

internal class ConeTypeInfo(
    coneType: ConeKotlinType,
    private val typeCheckerContext: ConeTypeCheckerContext,
    private val validityToken: Invalidatable
) : TypeInfo() {
    private val coneTypeWeakRef = WeakReference(coneType)

    override fun isClassType(): Boolean {
        assertIsValid()
        return coneType is ConeClassLikeType
    }

    override fun isErrorType(): Boolean {
        assertIsValid()
        return coneType is ConeClassErrorType
    }

    override fun classIdIfClassTypeOrError(): ClassId {
        assertIsValid()
        return when (val coneType = coneType) {
            is ConeClassLikeTypeImpl -> coneType.lookupTag.classId
            is ConeClassErrorType -> throw ErrorTypeClassIdAccessException()
            else -> throw ClassTypeExpectedException()
        }
    }

    override fun asDenotableTypeStringRepresentation(): String {
        assertIsValid()
        return coneType.render()//TODO
    }

    override fun isEqualTo(other: TypeInfo): Boolean {
        assertIsValid()
        other.assertIsValid()
        check(other is ConeTypeInfo)
        return AbstractTypeChecker.equalTypes(
            typeCheckerContext as AbstractTypeCheckerContext,
            coneType,
            other.coneType
        )
    }

    override fun isSubTypeOf(superType: TypeInfo): Boolean {
        assertIsValid()
        superType.assertIsValid()
        check(superType is ConeTypeInfo)
        return AbstractTypeChecker.isSubtypeOf(
            typeCheckerContext as AbstractTypeCheckerContext,
            coneType,
            superType.coneType
        )
    }

    override fun isDefinitelyNullable(): Boolean {
        assertIsValid()
        return coneType.nullability == ConeNullability.NULLABLE
    }

    override fun isDefinitelyNotNull(): Boolean {
        assertIsValid()
        return coneType.nullability == ConeNullability.NOT_NULL
    }

    override fun isValid(): Boolean {
        if (coneTypeWeakRef.get() == null) return false
        return validityToken.isValid()
    }

    override fun invalidationReason(): String {
        if (coneTypeWeakRef.get() == null) return "Cone type was garbage collected"
        return validityToken.invalidationReason()
    }

    private inline val coneType
        get() = coneTypeWeakRef.get() ?: if (validityToken.isValid()) {
            error("Cone type was garbage collected while analysis session is still valid")
        } else {
            error("Accessing the invalid coneType")
        }
}