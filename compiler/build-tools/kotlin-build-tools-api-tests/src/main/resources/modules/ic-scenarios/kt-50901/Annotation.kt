/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.serialization.SerialInfo

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ConfigurationField(
    val description: String,
    val min: Long = Long.MIN_VALUE,
    val max: Long = Long.MAX_VALUE
)
