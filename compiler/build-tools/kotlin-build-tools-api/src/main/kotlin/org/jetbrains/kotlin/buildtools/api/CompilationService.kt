/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * A facade for invoking compilation in Kotlin compiler. It allows to use compiler in different modes.
 * TODO: add a mention where to see the available modes after implementing them
 */
public interface CompilationService {
    public fun compile()

    public companion object {
        @JvmStatic
        public fun loadImplementation(classLoader: ClassLoader): CompilationService =
            loadImplementation(CompilationService::class, classLoader)
    }
}