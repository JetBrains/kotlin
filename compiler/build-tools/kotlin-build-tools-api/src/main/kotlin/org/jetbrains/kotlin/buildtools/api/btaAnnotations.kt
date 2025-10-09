/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

@RequiresOptIn("This part of the Build Tools API is experimental and might change in the future releases")
public annotation class ExperimentalBuildToolsApi

@RequiresOptIn("This compiler argument was deprecated in a previous Kotlin compiler version. Please see the argument KDoc for more information.")
public annotation class DeprecatedCompilerArgument

@RequiresOptIn("This compiler argument was removed in a previous Kotlin compiler version. Please see the argument KDoc for more information.")
public annotation class RemovedCompilerArgument
