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

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.XmlSerializerUtil
import com.sampullara.cli.Args
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

@JvmOverloads fun <A : CommonCompilerArguments> parseArguments(args: Array<String>, arguments: A, ignoreInvalidArguments: Boolean = false) {
    val unparsedArgs = Args.parse(arguments, args, false)
    val (unknownExtraArgs, unknownArgs) = unparsedArgs.partition { it.startsWith("-X") }
    arguments.unknownExtraFlags = unknownExtraArgs
    arguments.freeArgs = if (ignoreInvalidArguments) unknownArgs.filterNot { it.startsWith("-") } else unknownArgs

    if (!ignoreInvalidArguments) {
        for (argument in unknownArgs) {
            if (argument.startsWith("-")) {
                throw IllegalArgumentException("Invalid argument: " + argument)
            }
        }
    }
}

fun <T : Any> copyBean(bean: T) = copyFields(bean, bean.javaClass.newInstance(), true, collectFieldsToCopy(bean.javaClass, false))

fun <From : Any, To : From> mergeBeans(from: From, to: To): To {
    // TODO: rewrite when updated version of com.intellij.util.xmlb is available on TeamCity
    return copyFields(from, XmlSerializerUtil.createCopy(to), false, collectFieldsToCopy(from.javaClass, false))
}

fun <From : Any, To : Any> copyInheritedFields(from: From, to: To) = copyFields(from, to, true, collectFieldsToCopy(from.javaClass, true))

fun <From : Any, To : Any> copyFieldsSatisfying(from: From, to: To, predicate: (Field) -> Boolean) =
        copyFields(from, to, true, collectFieldsToCopy(from.javaClass, false).filter(predicate))

private fun <From : Any, To : Any> copyFields(from: From, to: To, deepCopyWhenNeeded: Boolean, fieldsToCopy: List<Field>): To {
    if (from == to) return to

    for (fromField in fieldsToCopy) {
        val toField = to.javaClass.getField(fromField.name)
        val fromValue = fromField.get(from)
        toField.set(to, if (deepCopyWhenNeeded) fromValue?.copyValueIfNeeded() else fromValue)
    }
    return to
}

private fun Any.copyValueIfNeeded(): Any {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ByteArray -> Arrays.copyOf(this, size)
        is CharArray -> Arrays.copyOf(this, size)
        is ShortArray -> Arrays.copyOf(this, size)
        is IntArray -> Arrays.copyOf(this, size)
        is LongArray -> Arrays.copyOf(this, size)
        is FloatArray -> Arrays.copyOf(this, size)
        is DoubleArray -> Arrays.copyOf(this, size)
        is BooleanArray -> Arrays.copyOf(this, size)

        is Array<*> -> java.lang.reflect.Array.newInstance(javaClass.componentType, size).apply {
            this as Array<Any?>
            (this@copyValueIfNeeded as Array<Any?>).forEachIndexed { i, value -> this[i] = value?.copyValueIfNeeded() }
        }

        is MutableCollection<*> -> (this as Collection<Any?>).mapTo(javaClass.newInstance() as MutableCollection<Any?>) { it?.copyValueIfNeeded() }

        is MutableMap<*, *> -> (javaClass.newInstance() as MutableMap<Any?, Any?>).apply {
            for ((k, v) in this@copyValueIfNeeded.entries) {
                put(k?.copyValueIfNeeded(), v?.copyValueIfNeeded())
            }
        }

        else -> this
    }
}

private fun collectFieldsToCopy(clazz: Class<*>, inheritedOnly: Boolean): List<Field> {
    val fromFields = ArrayList<Field>()

    var currentClass: Class<*>? = if (inheritedOnly) clazz.superclass else clazz
    while (currentClass != null) {
        for (field in currentClass.declaredFields) {
            val modifiers = field.modifiers
            if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                fromFields.add(field)
            }
        }
        currentClass = currentClass.superclass
    }

    return fromFields
}
