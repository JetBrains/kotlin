/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common.environment

import com.intellij.openapi.util.SystemInfo

fun setIdeaIoUseFallback() {
    val properties = System.getProperties()

    properties.setProperty("idea.io.use.nio2", java.lang.Boolean.TRUE.toString())

    if (!(SystemInfo.isJavaVersionAtLeast(1, 7, 0) && "1.7.0-ea" != SystemInfo.JAVA_VERSION)) {
        properties.setProperty("idea.io.use.fallback", java.lang.Boolean.TRUE.toString())
    }
}
