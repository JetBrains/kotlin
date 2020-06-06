/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.name.ClassId

abstract class TypeInfo : Invalidatable {
    abstract fun isClassType(): Boolean
    abstract fun classIdIfClassTypeOrError(): ClassId

    abstract fun isErrorType(): Boolean

    abstract fun asDenotableTypeStringRepresentation(): String

    abstract fun isEqualTo(other: TypeInfo): Boolean
    abstract fun isSubTypeOf(superType: TypeInfo): Boolean

    abstract fun isDefinitelyNullable(): Boolean
    abstract fun isDefinitelyNotNull(): Boolean

    override fun toString(): String = asDenotableTypeStringRepresentation()
}

class ErrorTypeClassIdAccessException(override val message: String? = null) : IllegalStateException()
class ClassTypeExpectedException(override val message: String? = null) : IllegalStateException()