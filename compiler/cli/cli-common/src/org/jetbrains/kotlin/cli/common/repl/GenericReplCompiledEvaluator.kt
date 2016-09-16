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

import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

open class GenericReplCompiledEvaluator(baseClasspath: Iterable<File>, baseClassloader: ClassLoader?) : ReplCompiledEvaluator {

    private var classLoader: org.jetbrains.kotlin.cli.common.repl.ReplClassLoader =
            org.jetbrains.kotlin.cli.common.repl.ReplClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))
    private val classLoaderLock = ReentrantReadWriteLock()

    private class ClassWithInstance(val klass: Class<*>, val instance: Any)

    private val compiledLoadedClassesHistory = arrayListOf<Pair<ReplCodeLine, ClassWithInstance>>()

    override fun eval(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>, compiledClasses: List<CompiledClassData>, hasResult: Boolean, newClasspath: List<File>): ReplEvalResult {

        checkAndUpdateReplHistoryCollection(compiledLoadedClassesHistory, history)?.let {
            return@eval ReplEvalResult.HistoryMismatch(it)
        }

        classLoaderLock.read {
            if (newClasspath.isNotEmpty()) {
                classLoaderLock.write {
                    classLoader = org.jetbrains.kotlin.cli.common.repl.ReplClassLoader(URLClassLoader(newClasspath.map { it.toURI().toURL() }.toTypedArray(), classLoader))
                }
            }
            compiledClasses.filter { it.path.endsWith(".class") }
                    .forEach {
                        classLoader.addClass(JvmClassName.byInternalName(it.path.replaceFirst("\\.class$".toRegex(), "")), it.bytes)
                    }
        }

        val scriptClass = classLoaderLock.read { classLoader.loadClass("Line${codeLine.no}") }

        val constructorParams = compiledLoadedClassesHistory.map { it.second.klass }.toTypedArray()
        val constructorArgs = compiledLoadedClassesHistory.map { it.second.instance }.toTypedArray()

        val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)
        val scriptInstance =
                try {
                    evalWithIO { scriptInstanceConstructor.newInstance(*constructorArgs) }
                }
                catch (e: Throwable) {
                    // ignore everything in the stack trace until this constructor call
                    return ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"))
                }

        compiledLoadedClassesHistory.add(codeLine to ClassWithInstance(scriptClass, scriptInstance))

        val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
        val rv: Any? = rvField.get(scriptInstance)

        return if (hasResult) ReplEvalResult.ValueResult(rv) else ReplEvalResult.UnitResult
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}