/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.utils.IDEAPlatforms.*
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

@Suppress("unused", "UNUSED_PARAMETER")
@IDEAPluginsCompatibilityAPI(
    _212, _213, _221,
    message = "Please migrate to the org.jetbrains.kotlin.types.error.ErrorUtils",
    plugins = "android/safeargs"
)
object ErrorUtils {
    @IDEAPluginsCompatibilityAPI(
        _212, _213, _221,
        message = "Please migrate to the org.jetbrains.kotlin.types.error.ErrorUtils",
        plugins = "android/safeargs"
    )
    @JvmStatic
    fun createUnresolvedType(presentableName: String, arguments: List<TypeProjection>): SimpleType {
        return org.jetbrains.kotlin.types.error.ErrorUtils.createErrorTypeWithArguments(
            ErrorTypeKind.UNRESOLVED_TYPE, arguments, this.toString()
        )
    }

    @IDEAPluginsCompatibilityAPI(
        _213, _221,
        message = "Please migrate to the org.jetbrains.kotlin.types.error.ErrorUtils",
        plugins = "mobile-ide/kotlin-ocswift"
    )
    @JvmStatic
    fun createErrorType(debugMessage: String): SimpleType {
        return org.jetbrains.kotlin.types.error.ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, debugMessage)
    }
}