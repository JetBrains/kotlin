/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.serialization.deserialization.EnumEntriesDeserializationSupport

class EnumEntriesDeserializationSupportImpl(
    private val platform: TargetPlatform?,
) : EnumEntriesDeserializationSupport {
    override fun canSynthesizeEnumEntries(): Boolean = platform.isJvm()
}