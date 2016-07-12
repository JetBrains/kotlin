/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin

import kotlin.internal.PureReifiable

/**
 * Returns an empty array of the specified type [T].
 */
public inline fun <reified @PureReifiable T> emptyArray(): Array<T> =
        @Suppress("UNCHECKED_CAST")
        (arrayOfNulls<T>(0) as Array<T>)
