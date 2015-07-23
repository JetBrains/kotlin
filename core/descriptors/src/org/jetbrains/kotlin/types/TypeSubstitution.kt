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

package org.jetbrains.kotlin.types

import kotlin.platform.platformStatic

public abstract class TypeSubstitution {
    companion object {
        platformStatic public val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: TypeConstructor) = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }

    public abstract fun get(key: TypeConstructor): TypeProjection?

    public open fun isEmpty(): Boolean = false

    public open fun approximateCapturedTypes(): Boolean = false
}
