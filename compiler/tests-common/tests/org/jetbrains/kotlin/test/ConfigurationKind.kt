/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

enum class ConfigurationKind(
        val withRuntime: Boolean = false,
        val withMockRuntime: Boolean = false,
        val withReflection: Boolean = false
) {
    /** JDK without any kotlin runtime */
    JDK_NO_RUNTIME(),
    /** JDK + light mock kotlin runtime */
    JDK_ONLY(withMockRuntime = true),
    /** JDK + kotlin runtime but without reflection */
    NO_KOTLIN_REFLECT(withRuntime = true),
    /** JDK + kotlin runtime + kotlin reflection */
    ALL(withRuntime = true, withReflection = true)
}
