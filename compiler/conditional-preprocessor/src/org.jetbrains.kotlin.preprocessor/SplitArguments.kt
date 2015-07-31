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

package org.jetbrains.kotlin.preprocessor

import org.jetbrains.kotlin.psi.JetStringTemplateEntry
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

data class SplitArguments(val positional: List<ValueArgument>, val named: List<ValueArgument>)
{
    fun get(position: Int, name: String): ValueArgument? =
            positional.getOrNull(position) ?: named.find { it.getArgumentName()!!.asName.asString() == name }
}

fun List<ValueArgument>.splitToPositionalAndNamed(): SplitArguments {
    val (positional, named) = partition { !it.isNamed() }
    return SplitArguments(positional, named)
}

fun ValueArgument.parseIntegerValue(): Int = getArgumentExpression()!!.text.toInt()
fun ValueArgument.parseStringValue(): String = getArgumentExpression()!!.getChildrenOfType<JetStringTemplateEntry>().joinToString("") { it.text }