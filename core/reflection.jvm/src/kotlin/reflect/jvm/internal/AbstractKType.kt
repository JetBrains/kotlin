/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.jvm.internal.KTypeBase
import kotlin.reflect.KType

internal abstract class AbstractKType : KTypeBase {
    abstract fun isSubtypeOf(other: AbstractKType): Boolean

    abstract fun makeNullableAsSpecified(nullable: Boolean): AbstractKType

    abstract fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType

    abstract val abbreviation: KType?

    abstract val isDefinitelyNotNullType: Boolean

    abstract val isNothingType: Boolean
    abstract val isMutableCollectionType: Boolean
    abstract val isSuspendFunctionType: Boolean
    abstract val isRawType: Boolean
    abstract fun lowerBoundIfFlexible(): AbstractKType?
    abstract fun upperBoundIfFlexible(): AbstractKType?
}
