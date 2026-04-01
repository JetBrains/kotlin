/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi

import org.jetbrains.kotlin.abi.tools.KlibTarget
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId

internal object AbiValidationUtils {
    fun convert(filters: AbiFilters): org.jetbrains.kotlin.abi.tools.AbiFilters {
        return org.jetbrains.kotlin.abi.tools.AbiFilters(
            filters[AbiFilters.INCLUDE_NAMED],
            filters[AbiFilters.EXCLUDE_NAMED],
            filters[AbiFilters.INCLUDE_ANNOTATED_WITH],
            filters[AbiFilters.EXCLUDE_ANNOTATED_WITH]
        )
    }

    fun convert(target: KlibTargetId): KlibTarget {
        return KlibTarget(target.targetType.canonicalName, target.customizedName)
    }
}
