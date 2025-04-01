/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData

interface DeserializedContainerSource : SourceElement {
    // Non-null if this container is loaded from a class with an incompatible binary version
    val incompatibility: IncompatibleVersionErrorData<*>?

    val preReleaseInfo: PreReleaseInfo

    // True iff this container was compiled by the new IR backend, this compiler is not using the IR backend right now,
    // and no additional flags to override this behavior were specified.
    val abiStability: DeserializedContainerAbiStability

    // This string should only be used in error messages
    val presentableString: String
}

enum class DeserializedContainerAbiStability {
    // Either the container is stable, or this compiler is configured to ignore ABI stability of dependencies.
    STABLE,

    // The container is unstable because either:
    // 1) it is compiled with JVM IR prior to 1.4.30, or
    // 2) it is compiled with JVM IR >= 1.4.30 with the `-Xabi-stability=unstable` compiler option,
    // 3) it is compiled with FIR prior to 2.0.0,
    // and this compiler is _not_ configured to ignore that.
    UNSTABLE,
}

/**
 * @property isInvisible True if this container is "invisible" because it has a pre-release flag.
 * @property poisoningFeatures The list of manually enabled language features that caused raising the pre-release flag.
 */
data class PreReleaseInfo(val isInvisible: Boolean, val poisoningFeatures: List<String> = emptyList()) {
    companion object {
        val DEFAULT_VISIBLE = PreReleaseInfo(isInvisible = false)
    }
}
