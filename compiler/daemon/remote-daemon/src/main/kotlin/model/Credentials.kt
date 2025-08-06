/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Serializable

@Serializable
data class Credentials(
    val allowed: Set<String> = emptySet(),
)

@Serializable
data class UUIDMapping(
    val credential: String,
    val userId: String
)