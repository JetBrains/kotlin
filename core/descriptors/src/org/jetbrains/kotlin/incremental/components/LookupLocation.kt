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

package org.jetbrains.kotlin.incremental.components

import kotlin.platform.platformStatic

public interface LookupLocation {

    companion object {
        @deprecated("Use more suitable constant if possible")
        val NO_LOCATION = NoLookupLocation.create("(unsorted)")
        val NO_LOCATION_FROM_IDE = NoLookupLocation.create("from IDE")
        val NO_LOCATION_FROM_BACKEND = NoLookupLocation.create("from backend")
        val NO_LOCATION_FROM_TEST = NoLookupLocation.create("from test")
    }
}

public class NoLookupLocation private constructor(private val reason: String) : LookupLocation {
    override fun toString() = "NO LOCATION $reason"

    companion object {
        platformStatic
        public fun create(reason: String): LookupLocation = NoLookupLocation(reason)
    }
}
