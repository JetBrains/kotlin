/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

/**
 * Used for marking API used in the legacy K2 CLI pipeline.
 */
@RequiresOptIn("Consider using the new pipeline API from `org.jetbrains.kotlin.cli.pipeline.jvm`")
annotation class LegacyK2CliPipeline
