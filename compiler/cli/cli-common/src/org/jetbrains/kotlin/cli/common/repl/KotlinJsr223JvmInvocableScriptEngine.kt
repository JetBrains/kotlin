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

import javax.script.Invocable
import javax.script.ScriptException

@Suppress("unused") // used externally (kotlin.script.utils)
interface KotlinJsr223JvmInvocableScriptEngine : Invocable {

    val replScriptInvoker: ReplScriptInvoker

    override fun invokeFunction(name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("function name cannot be null")
        return processInvokeResult(replScriptInvoker.invokeFunction(name, *args), isMethod = true)
    }

    override fun invokeMethod(thiz: Any?, name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("method name cannot be null")
        if (thiz == null) throw IllegalArgumentException("cannot invoke method on the null object")
        return processInvokeResult(replScriptInvoker.invokeMethod(thiz, name, *args), isMethod = true)
    }

    override fun <T : Any> getInterface(clasz: Class<T>?): T? {
        if (clasz == null) throw IllegalArgumentException("class object cannot be null")
        if (!clasz.isInterface) throw IllegalArgumentException("expecting interface")
        return processInvokeResult(replScriptInvoker.getInterface(clasz.kotlin), isMethod = false) as? T
    }

    override fun <T : Any> getInterface(thiz: Any?, clasz: Class<T>?): T? {
        if (thiz == null) throw IllegalArgumentException("object cannot be null")
        if (clasz == null) throw IllegalArgumentException("class object cannot be null")
        if (!clasz.isInterface) throw IllegalArgumentException("expecting interface")
        return processInvokeResult(replScriptInvoker.getInterface(thiz, clasz.kotlin), isMethod = false) as? T
    }

    private fun processInvokeResult(res: ReplScriptInvokeResult, isMethod: Boolean): Any? =
            when (res) {
                is ReplScriptInvokeResult.Error.NoSuchEntity -> throw if (isMethod) NoSuchMethodException(res.message) else IllegalArgumentException(res.message)
                is ReplScriptInvokeResult.Error.CompileTime -> throw IllegalArgumentException(res.message) // should not happen in the current code, so leaving it here despite the contradiction with Invocable's specs
                is ReplScriptInvokeResult.Error.Runtime -> throw ScriptException(res.message)
                is ReplScriptInvokeResult.Error -> throw ScriptException(res.message)
                is ReplScriptInvokeResult.UnitResult -> Unit // TODO: check if it is suitable replacement for java's Void
                is ReplScriptInvokeResult.ValueResult -> res.value
            }
}