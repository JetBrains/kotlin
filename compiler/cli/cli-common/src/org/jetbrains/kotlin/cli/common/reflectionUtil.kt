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

package org.jetbrains.kotlin.cli.common

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType

fun tryConstructScriptClass(scriptClass: Class<*>, scriptArgs: List<String>): Any? {

    fun convertPrimitive(type: KType?, arg: String): Any? =
            when (type?.classifier) {
                String::class -> arg
                Int::class -> arg.toInt()
                Long::class -> arg.toLong()
                Short::class -> arg.toShort()
                Byte::class -> arg.toByte()
                Char::class -> arg[0]
                Float::class -> arg.toFloat()
                Double::class -> arg.toDouble()
                Boolean::class -> arg.toBoolean()
                else -> null
            }

    fun convertArray(type: KType?, args: List<String>): Any? =
            when (type?.classifier) {
                String::class -> args.toTypedArray()
                Int::class -> args.map(String::toInt).toTypedArray()
                Long::class -> args.map(String::toLong).toTypedArray()
                Short::class -> args.map(String::toShort).toTypedArray()
                Byte::class -> args.map(String::toByte).toTypedArray()
                Char::class -> args.map { it[0] }.toTypedArray()
                Float::class -> args.map(String::toFloat).toTypedArray()
                Double::class -> args.map(String::toDouble).toTypedArray()
                Boolean::class -> args.map(String::toBoolean).toTypedArray()
                else -> null
            }

    fun foldingFunc(state: Pair<List<Any>, List<String>?>, par: KParameter): Pair<List<Any>, List<String>?> {
        state.second?.let { scriptArgsLeft ->
            try {
                if (scriptArgsLeft.isNotEmpty()) {
                    val primArgCandidate = convertPrimitive(par.type, scriptArgsLeft.first())
                    if (primArgCandidate != null)
                        return@foldingFunc Pair(state.first + primArgCandidate, scriptArgsLeft.drop(1))
                }

                if ((par.type.classifier as? KClass<*>)?.qualifiedName == Array<Any>::class.qualifiedName) {
                    val arrCompType = par.type.arguments.getOrNull(0)?.type
                    val arrayArgCandidate = convertArray(arrCompType, scriptArgsLeft)
                    if (arrayArgCandidate != null)
                        return@foldingFunc Pair(state.first + arrayArgCandidate, null)
                }
            }
            catch (e: NumberFormatException) {
            } // just skips to return below
        }
        return state
    }

    try {
        return scriptClass.getConstructor(Array<String>::class.java).newInstance(scriptArgs.toTypedArray())
    }
    catch (e: NoSuchMethodException) {
        for (ctor in scriptClass.kotlin.constructors) {
            val (ctorArgs, scriptArgsLeft) = ctor.parameters.fold(Pair(emptyList<Any>(), scriptArgs), ::foldingFunc)
            if (ctorArgs.size <= ctor.parameters.size && (scriptArgsLeft == null || scriptArgsLeft.isEmpty())) {
                val argsMap = ctor.parameters.zip(ctorArgs).toMap()
                try {
                    return ctor.callBy(argsMap)
                }
                catch (e: Exception) { // TODO: find the exact exception type thrown then callBy fails
                }
            }
        }
    }
    return null
}


