/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

internal object DefaultKotlinLogger : KotlinLogger {
    override val isDebugEnabled: Boolean
        get() = TODO("Default KotlinLogger is not yet implemented in the Build Tools API")

    override fun error(msg: String, throwable: Throwable?) {
        TODO("Default KotlinLogger is not yet implemented in the Build Tools API")
    }

    override fun warn(msg: String) {
        TODO("Default KotlinLogger is not yet implemented in the Build Tools API")
    }

    override fun info(msg: String) {
        TODO("Default KotlinLogger is not yet implemented in the Build Tools API")
    }

    override fun debug(msg: String) {
        TODO("Default KotlinLogger is not yet implemented in the Build Tools API")
    }

    override fun lifecycle(msg: String) {
        TODO("Default KotlinLogger is not yet implemented in the Build Tools API")
    }
}