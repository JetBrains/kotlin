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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.types.error.ErrorUtils

interface ArgumentMapping {
    fun isError(): Boolean
}

object ArgumentUnmapped : ArgumentMapping {
    override fun isError(): Boolean = true
}

enum class ArgumentMatchStatus(val isError: Boolean = true) {
    SUCCESS(false),
    TYPE_MISMATCH,
    UNKNOWN
}

interface ArgumentMatch : ArgumentMapping {
    val valueParameter: ValueParameterDescriptor
    val status: ArgumentMatchStatus

    override fun isError(): Boolean = status.isError
}

class ArgumentMatchImpl(override val valueParameter: ValueParameterDescriptor) : ArgumentMatch {
    private var _status: ArgumentMatchStatus? = null

    override val status: ArgumentMatchStatus
        get() = _status ?: ArgumentMatchStatus.UNKNOWN

    fun recordMatchStatus(status: ArgumentMatchStatus) {
        _status = status
    }
}

//TODO: temporary hack until status.isSuccess is not always correct
fun ResolvedCall<*>.isReallySuccess(): Boolean = status.isSuccess && !ErrorUtils.isError(resultingDescriptor)
