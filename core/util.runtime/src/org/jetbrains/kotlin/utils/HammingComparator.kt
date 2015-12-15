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

package org.jetbrains.kotlin.utils

import java.util.*

class HammingComparator<T>(private val referenceString: String, private val asString: T.() -> String) : Comparator<T> {
    private fun countDifference(s1: String): Int {
        return (0..Math.min(s1.lastIndex, referenceString.lastIndex)).count { s1[it] != referenceString[it] }
    }

    override fun compare(lookupItem1: T, lookupItem2: T): Int {
        return countDifference(lookupItem1.asString()) - countDifference(lookupItem2.asString())
    }
}
