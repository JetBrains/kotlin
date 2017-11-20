/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    ALL(withRuntime = true, withReflection = true),
}
