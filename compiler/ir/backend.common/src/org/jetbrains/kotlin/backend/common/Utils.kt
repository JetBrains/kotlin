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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.getCompilerMessageLocation

fun CommonBackendContext.reportWarning(message: String, irFile: IrFile, irElement: IrElement) {
    val location = irElement.getCompilerMessageLocation(irFile)
    messageCollector.report(CompilerMessageSeverity.WARNING, message, location)
}

fun <E> MutableList<E>.push(element: E) = this.add(element)

fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

fun <E> MutableList<E>.peek(): E? = if (size == 0) null else this[size - 1]

fun <T> Collection<T>.atMostOne(): T? {
    return when (this.size) {
        0 -> null
        1 -> this.iterator().next()
        else -> throw IllegalArgumentException("Collection has more than one element.")
    }
}

inline fun <T> Iterable<T>.atMostOne(predicate: (T) -> Boolean): T? = this.filter(predicate).atMostOne()

fun <T: Any> T.onlyIf(condition: T.()->Boolean, then: (T)->Unit): T {
    if (this.condition()) then(this)
    return this
}
