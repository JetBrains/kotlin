/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

interface DeserializationConfiguration {
    val skipMetadataVersionCheck: Boolean
        get() = false

    val reportErrorsOnPreReleaseDependencies: Boolean
        get() = false

    val reportErrorsOnIrDependencies: Boolean
        get() = false

    val typeAliasesAllowed: Boolean
        get() = true

    val isJvmPackageNameSupported: Boolean
        get() = true

    val readDeserializedContracts: Boolean
        get() = false

    val releaseCoroutines: Boolean
        get() = false

    object Default : DeserializationConfiguration
}
