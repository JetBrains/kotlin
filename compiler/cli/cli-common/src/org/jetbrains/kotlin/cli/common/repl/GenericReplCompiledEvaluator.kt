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

import org.jetbrains.kotlin.cli.common.tryCreateCallableMapping
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.*

open class GenericReplCompiledEvaluator(baseClasspath: Iterable<File>,
                                        baseClassloader: ClassLoader?,
                                        val scriptArgs: Array<Any?>? = null,
                                        val scriptArgsTypes: Array<Class<*>>? = null
) : ReplCompiledEvaluator, ReplScriptInvoker {

    private var classLoader: org.jetbrains.kotlin.cli.common.repl.ReplClassLoader = makeReplClassLoader(baseClassloader, baseClasspath)
    private val classLoaderLock = ReentrantReadWriteLock()

    // TODO: consider to expose it as a part of (evaluator, invoker) interface
    private val evalStateLock = ReentrantReadWriteLock()

    private data class ClassWithInstance(val klass: Class<*>, val instance: Any)

    private val compiledLoadedClassesHistory = ReplHistory<ClassWithInstance>()

    override fun eval(codeLine: ReplCodeLine, history: List<ReplCodeLine>, compiledClasses: List<CompiledClassData>, hasResult: Boolean, classpathAddendum: List<File>): ReplEvalResult /*= evalStateLock.write*/ {
        checkAndUpdateReplHistoryCollection(compiledLoadedClassesHistory, history)?.let {
            return@eval ReplEvalResult.HistoryMismatch(compiledLoadedClassesHistory.lines, it)
        }

        var mainLineClassName: String? = null

        fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.replaceFirst("\\.class$".toRegex(), ""))

        classLoaderLock.read {
            if (classpathAddendum.isNotEmpty()) {
                classLoaderLock.write {
                    classLoader = makeReplClassLoader(classLoader, classpathAddendum)
                }
            }
            compiledClasses.filter { it.path.endsWith(".class") }
                    .forEach {
                        val className = classNameFromPath(it.path)
                        if (className.fqNameForClassNameWithoutDollars.shortName().asString() == "Line${codeLine.no}") {
                            mainLineClassName = className.fqNameForClassNameWithoutDollars.asString()
                        }
                        classLoader.addClass(className, it.bytes)
                    }
        }

        val scriptClass = classLoaderLock.read {
            try {
                classLoader.loadClass(mainLineClassName!!)
            }
            catch (e: Throwable) {
                return ReplEvalResult.Error.Runtime(compiledLoadedClassesHistory.lines, "Error loading class $mainLineClassName: known classes: ${compiledClasses.map { classNameFromPath(it.path).fqNameForClassNameWithoutDollars.asString() }}",
                                                    e as? Exception)
            }
        }

        val constructorParams: Array<Class<*>> =
                (compiledLoadedClassesHistory.values.map { it.klass } +
                 (scriptArgs?.mapIndexed { i, it -> scriptArgsTypes?.getOrNull(i) ?: it?.javaClass ?: Any::class.java } ?: emptyList())
                ).toTypedArray()
        val constructorArgs: Array<Any?> = (compiledLoadedClassesHistory.values.map { it.instance } + scriptArgs.orEmpty()).toTypedArray()

        val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)
        val scriptInstance =
                try {
                    evalWithIO { scriptInstanceConstructor.newInstance(*constructorArgs) }
                }
                catch (e: Throwable) {
                    // ignore everything in the stack trace until this constructor call
                    return ReplEvalResult.Error.Runtime(compiledLoadedClassesHistory.lines, renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e as? Exception)
                }

        compiledLoadedClassesHistory.add(codeLine, ClassWithInstance(scriptClass, scriptInstance))

        val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
        val rv: Any? = rvField.get(scriptInstance)

        return if (hasResult) ReplEvalResult.ValueResult(compiledLoadedClassesHistory.lines, rv) else ReplEvalResult.UnitResult(compiledLoadedClassesHistory.lines)
    }

    override fun <T: Any> getInterface(klass: KClass<T>): ReplScriptInvokeResult = evalStateLock.read {
        val (_, instance) = compiledLoadedClassesHistory.values.lastOrNull() ?: return ReplScriptInvokeResult.Error.NoSuchEntity("no script ")
        return getInterface(instance, klass)
    }

    override fun <T: Any> getInterface(receiver: Any, klass: KClass<T>): ReplScriptInvokeResult = evalStateLock.read {
        return ReplScriptInvokeResult.ValueResult(klass.safeCast(receiver))
    }

    override fun invokeMethod(receiver: Any, name: String, vararg args: Any?): ReplScriptInvokeResult = evalStateLock.read {
        return invokeImpl(receiver.javaClass.kotlin, receiver, name, args)
    }

    override fun invokeFunction(name: String, vararg args: Any?): ReplScriptInvokeResult = evalStateLock.read {
        val (klass, instance) = compiledLoadedClassesHistory.values.lastOrNull() ?: return ReplScriptInvokeResult.Error.NoSuchEntity("no script ")
        return invokeImpl(klass.kotlin, instance, name, args)
    }

    private fun invokeImpl(receiverClass: KClass<*>, receiverInstance: Any, name: String, args: Array<out Any?>): ReplScriptInvokeResult {

        val candidates = receiverClass.memberFunctions.filter { it.name == name } +
                         receiverClass.memberExtensionFunctions.filter { it.name == name }
        val (fn, mapping) = candidates.findMapping(args.toList()) ?:
                            candidates.findMapping(listOf<Any?>(receiverInstance) + args) ?:
                            return ReplScriptInvokeResult.Error.NoSuchEntity("no suitable function '$name' found")
        val res = try {
            evalWithIO { fn.callBy(mapping) }
        }
        catch (e: Throwable) {
            // ignore everything in the stack trace until this constructor call
            return ReplScriptInvokeResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${fn.name}"), e as? Exception)
        }
        return if (fn.returnType.classifier == Unit::class) ReplScriptInvokeResult.UnitResult else ReplScriptInvokeResult.ValueResult(res)
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}

private fun makeReplClassLoader(baseClassloader: ClassLoader?, baseClasspath: Iterable<File>) =
        ReplClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))

private fun Iterable<KFunction<*>>.findMapping(args: List<Any?>): Pair<KFunction<*>, Map<KParameter, Any?>>? {
    for (fn in this) {
        val mapping = tryCreateCallableMapping(fn, args)
        if (mapping != null) return fn to mapping
    }
    return null
}
