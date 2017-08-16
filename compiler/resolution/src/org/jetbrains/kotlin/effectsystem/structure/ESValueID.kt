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

package org.jetbrains.kotlin.effectsystem.structure

import java.util.*

/**
 * Interface which is used to determine identity of ESValues.
 * All implementors must override equals() and hashCode() methods.
 *
 * See also [DataFlowValueID] in 'compiler' module to see current
 * main implementation, which is based on DataFlowValues.
 */
interface ESValueID

class ConstantID(val value: Any?) : ESValueID {
    override fun equals(other: Any?): Boolean = other is ConstantID && other.value == value
    override fun hashCode(): Int = Objects.hashCode(value) * 31
    override fun toString(): String = value.toString()
}

/**
 * Singleton for special NON_NULL value
 */
object NOT_NULL_ID : ESValueID {
    override fun toString(): String = "<non-null value>"
}

object UNKNOWN_ID : ESValueID {
    override fun toString(): String = "<unknown value>"
}

object UNIT_ID : ESValueID {
    override fun toString(): String = "Unit"
}