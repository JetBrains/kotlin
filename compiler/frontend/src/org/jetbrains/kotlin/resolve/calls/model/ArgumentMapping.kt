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

public interface ArgumentMapping {
    public fun isError(): Boolean
}

public object ArgumentUnmapped: ArgumentMapping {
    override fun isError(): Boolean = true
}

public enum class ArgumentMatchStatus(val isError: Boolean = true) {
    SUCCESS(false),
    TYPE_MISMATCH(),
    ARGUMENT_HAS_NO_TYPE(),

    // The case when there is no type mismatch, but parameter has uninferred types:
    // fun <T> foo(l: List<T>) {}; val l = foo(emptyList())
    MATCH_MODULO_UNINFERRED_TYPES(),

    UNKNOWN()
}

public trait ArgumentMatch : ArgumentMapping {
    public val valueParameter: ValueParameterDescriptor
    public val status: ArgumentMatchStatus

    override fun isError(): Boolean = status.isError
}

class ArgumentMatchImpl(override val valueParameter: ValueParameterDescriptor): ArgumentMatch {
    private var _status: ArgumentMatchStatus? = null

    override val status: ArgumentMatchStatus
        get() = _status ?: ArgumentMatchStatus.UNKNOWN

    fun recordMatchStatus(status: ArgumentMatchStatus) {
        _status = status
    }

    fun replaceValueParameter(newValueParameter: ValueParameterDescriptor): ArgumentMatchImpl {
        val newArgumentMatch = ArgumentMatchImpl(newValueParameter)
        newArgumentMatch._status = _status
        return newArgumentMatch
    }
}
