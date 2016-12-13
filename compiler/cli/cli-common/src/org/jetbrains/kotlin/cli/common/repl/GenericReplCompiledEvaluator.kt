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

open class GenericReplCompiledEvaluator(baseClasspath: Iterable<File>,
                                        baseClassloader: ClassLoader?,
                                        val scriptArgs: Array<Any?>? = null,
                                        val scriptArgsTypes: Array<Class<*>>? = null
) : ReplCompiledEvaluator {

    private var classLoader: org.jetbrains.kotlin.cli.common.repl.ReplClassLoader = makeReplClassLoader(baseClassloader, baseClasspath)
    private val classLoaderLock = ReentrantReadWriteLock()

    // TODO: consider to expose it as a part of (evaluator, invoker) interface
    private val evalStateLock = ReentrantReadWriteLock()

    private val compiledLoadedClassesHistory = ReplHistory<ClassWithInstance>()

    override fun eval(codeLine: ReplCodeLine,
                      history: List<ReplCodeLine>,
                      compiledClasses: List<CompiledClassData>,
                      hasResult: Boolean,
                      classpathAddendum: List<File>,
                      invokeWrapper: InvokeWrapper?
    ): ReplEvalResult = evalStateLock.write {
        checkAndUpdateReplHistoryCollection(compiledLoadedClassesHistory, history)?.let {
            return@eval ReplEvalResult.HistoryMismatch(compiledLoadedClassesHistory.lines, it)
        }

        var mainLineClassName: String? = null

        fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.replaceFirst("\\.class$".toRegex(), ""))

        fun processCompiledClasses() {
            compiledClasses.filter { it.path.endsWith(".class") }
                    .forEach {
                        val className = classNameFromPath(it.path)
                        if (className.fqNameForClassNameWithoutDollars.shortName().asString() == "Line${codeLine.no}") {
                            mainLineClassName = className.fqNameForClassNameWithoutDollars.asString()
                        }
                        classLoader.addClass(className, it.bytes)
                    }
        }

        classLoaderLock.read {
            if (classpathAddendum.isNotEmpty()) {
                classLoaderLock.write {
                    classLoader = makeReplClassLoader(classLoader, classpathAddendum)
                }
            }
            processCompiledClasses()
        }

        fun compiledClassesNames() = compiledClasses.map { classNameFromPath(it.path).fqNameForClassNameWithoutDollars.asString() }

        val scriptClass = classLoaderLock.read {
            try {
                classLoader.loadClass(mainLineClassName!!)
            }
            catch (e: Throwable) {
                return ReplEvalResult.Error.Runtime(compiledLoadedClassesHistory.lines, "Error loading class $mainLineClassName: known classes: ${compiledClassesNames()}",
                                                    e as? Exception)
            }
        }

        fun getConstructorParams(): Array<Class<*>> =
                (compiledLoadedClassesHistory.values.map { it.klass.java } +
                 (scriptArgs?.mapIndexed { i, it -> scriptArgsTypes?.getOrNull(i) ?: it?.javaClass ?: Any::class.java } ?: emptyList())
                ).toTypedArray()
        fun getConstructorArgs() = (compiledLoadedClassesHistory.values.map { it.instance } + scriptArgs.orEmpty()).toTypedArray()

        val constructorParams: Array<Class<*>> = getConstructorParams()
        val constructorArgs: Array<Any?> = getConstructorArgs()

        val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)
        val scriptInstance =
                try {
                    invokeWrapper?.invoke { scriptInstanceConstructor.newInstance(*constructorArgs) } ?: scriptInstanceConstructor.newInstance(*constructorArgs)
                }
                catch (e: Throwable) {
                    // ignore everything in the stack trace until this constructor call
                    return ReplEvalResult.Error.Runtime(compiledLoadedClassesHistory.lines, renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e as? Exception)
                }

        compiledLoadedClassesHistory.add(codeLine, ClassWithInstance(scriptClass.kotlin, scriptInstance))

        val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
        val rv: Any? = rvField.get(scriptInstance)

        return if (hasResult) ReplEvalResult.ValueResult(compiledLoadedClassesHistory.lines, rv) else ReplEvalResult.UnitResult(compiledLoadedClassesHistory.lines)
    }

    override val lastEvaluatedScript: ClassWithInstance? get() = evalStateLock.read { compiledLoadedClassesHistory.values.lastOrNull() }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}

private fun makeReplClassLoader(baseClassloader: ClassLoader?, baseClasspath: Iterable<File>) =
        ReplClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))

