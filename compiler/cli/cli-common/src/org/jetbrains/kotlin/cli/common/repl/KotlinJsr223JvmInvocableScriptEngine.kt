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

package org.jetbrains.kotlin.cli.common.repl

import org.jetbrains.kotlin.utils.tryCreateCallableMapping
import javax.script.Invocable
import javax.script.ScriptException
import kotlin.reflect.*

@Suppress("unused") // used externally (kotlin.script.utils)
interface KotlinJsr223JvmInvocableScriptEngine : Invocable {

    val replScriptEvaluator: ReplEvaluatorBase

    fun <T: Any> getInterface(klass: KClass<T>): Any? {
        val (_, instance) = replScriptEvaluator.lastEvaluatedScript ?: throw IllegalArgumentException("no script ")
        return getInterface(instance, klass)
    }

    fun <T: Any> getInterface(receiver: Any, klass: KClass<T>): Any? {
        return klass.safeCast(receiver)
    }

    override fun invokeFunction(name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("function name cannot be null")
        val (klass, instance) = replScriptEvaluator.lastEvaluatedScript ?: throw IllegalArgumentException("no script ")
        return invokeImpl(klass, instance, name, args, invokeWrapper = null)
    }

    override fun invokeMethod(thiz: Any?, name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("method name cannot be null")
        if (thiz == null) throw IllegalArgumentException("cannot invoke method on the null object")
        return invokeImpl(thiz.javaClass.kotlin, thiz, name, args, invokeWrapper = null)
    }

    override fun <T : Any> getInterface(clasz: Class<T>?): T? {
        val (_, instance) = replScriptEvaluator.lastEvaluatedScript ?: throw IllegalArgumentException("no script ")
        return getInterface(instance, clasz)
    }

    override fun <T : Any> getInterface(thiz: Any?, clasz: Class<T>?): T? {
        if (thiz == null) throw IllegalArgumentException("object cannot be null")
        if (clasz == null) throw IllegalArgumentException("class object cannot be null")
        if (!clasz.isInterface) throw IllegalArgumentException("expecting interface")
        return clasz.kotlin.safeCast(thiz)
    }
}

private fun invokeImpl(receiverClass: KClass<*>, receiverInstance: Any, name: String, args: Array<out Any?>, invokeWrapper: InvokeWrapper?): Any? {

    val candidates = receiverClass.functions.filter { it.name == name }
    val (fn, mapping) = candidates.findMapping(listOf<Any?>(receiverInstance) + args) ?:
                        throw NoSuchMethodException("no suitable function '$name' found")
    val res = try {
        invokeWrapper?.invoke {
            fn.callBy(mapping)
        } ?: fn.callBy(mapping)
    }
    catch (e: Throwable) {
        // ignore everything in the stack trace until this constructor call
        throw ScriptException(renderReplStackTrace(e.cause!!, startFromMethodName = fn.name))
    }
    return if (fn.returnType.classifier == Unit::class) Unit else res
}

private fun Iterable<KFunction<*>>.findMapping(args: List<Any?>): Pair<KFunction<*>, Map<KParameter, Any?>>? {
    for (fn in this) {
        val mapping = tryCreateCallableMapping(fn, args)
        if (mapping != null) return fn to mapping
    }
    return null
}
