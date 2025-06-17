/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.internal

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Base class for options used by build operations and arguments.
 *
 * Note: two options with the same [id] will overwrite each other when applied to an option set.
 *
 * @param id an arbitrary, unique identifier for this option.
 * @param V the type of data that this option holds
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public abstract class BaseOption<V>(public val id: String) {
    override fun toString(): String = id
}
