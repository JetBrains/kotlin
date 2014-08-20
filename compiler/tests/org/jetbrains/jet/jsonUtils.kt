/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet

import com.google.gson.JsonObject

public fun JsonObject.getString(name: String): String {
    val member = getNullableString(name)
    if (member == null) {
        throw IllegalStateException("Member with name '$name' is expected in '$this'")
    }

    return member
}

public fun JsonObject.getNullableString(name: String): String? = this[name]?.getAsString()