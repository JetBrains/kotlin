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

public enum class ConfigurationKind(
        public val withJdkAnnotations: Boolean,
        public val withRuntime: Boolean,
        public val withReflection: Boolean
) {
    JDK_ONLY(withJdkAnnotations = false, withRuntime = false, withReflection = false),
    JDK_AND_ANNOTATIONS(withJdkAnnotations = true, withRuntime = false, withReflection = false),
    NO_KOTLIN_REFLECT(withJdkAnnotations = true, withRuntime = true, withReflection = false),
    ALL(withJdkAnnotations = true, withRuntime = true, withReflection = true),
}
