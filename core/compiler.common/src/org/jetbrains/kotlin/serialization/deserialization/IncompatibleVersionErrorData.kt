/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

data class IncompatibleVersionErrorData<out T>(
    val actualVersion: T,
    val compilerVersion: T?,
    val languageVersion: T?,
    val expectedVersion: T,
    val filePath: String,
) {
    init {
        require(compilerVersion != null && languageVersion != null || compilerVersion == null && languageVersion == null) {
            "${::compilerVersion.name} and ${::languageVersion.name} both must be null or both must be not-null"
        }
    }
}
