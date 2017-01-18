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

open class GenericReplEvaluator(baseClasspath: Iterable<File>,
                                baseClassloader: ClassLoader?,
                                protected val fallbackScriptArgs: ScriptArgsWithTypes? = null,
                                protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT,
                                protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : ReplEvaluator {

    private val topClassLoader: ReplClassLoader = makeReplClassLoader(baseClassloader, baseClasspath)

    private val evaluatedHistory = ReplHistory<EvalClassWithInstanceAndLoader>()

    override fun resetToLine(lineNumber: Int): List<ReplCodeLine> {
        return stateLock.write {
            evaluatedHistory.resetToLine(lineNumber)
        }.map { it.first }
    }

    override val history: List<ReplCodeLine> get() = stateLock.read { evaluatedHistory.copySources() }

    override val currentClasspath: List<File> get() = stateLock.read {
        evaluatedHistory.copyValues().lastOrNull()?.let { it.classLoader.listAllUrlsAsFiles() }
        ?: topClassLoader.listAllUrlsAsFiles()
    }

    private class HistoryActions(val effectiveHistory: List<EvalClassWithInstanceAndLoader>,
                                 val verify: (compareHistory: SourceList?) -> Int?,
                                 val addPlaceholder: (line: CompiledReplCodeLine, value: EvalClassWithInstanceAndLoader) -> Unit,
                                 val removePlaceholder: (line: CompiledReplCodeLine) -> Boolean,
                                 val addFinal: (line: CompiledReplCodeLine, value: EvalClassWithInstanceAndLoader) -> Unit,
                                 val processClasses: (compileResult: ReplCompileResponse.CompiledClasses) -> Pair<ClassLoader, Class<out Any>>)

    private fun prependClassLoaderWithNewClasses(effectiveHistory: List<EvalClassWithInstanceAndLoader>, compileResult: ReplCompileResponse.CompiledClasses): Pair<ClassLoader, Class<out Any>> {
        return stateLock.write {
            var mainLineClassName: String? = null
            val classLoader = makeReplClassLoader(effectiveHistory.lastOrNull()?.classLoader ?: topClassLoader, compileResult.classpathAddendum)
            fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.replaceFirst("\\.class$".toRegex(), ""))
            fun compiledClassesNames() = compileResult.classes.map { classNameFromPath(it.path).fqNameForClassNameWithoutDollars.asString() }
            val expectedClassName = compileResult.generatedClassname
            compileResult.classes.filter { it.path.endsWith(".class") }
                    .forEach {
                        val className = classNameFromPath(it.path)
                        if (className.internalName == expectedClassName || className.internalName.endsWith("/$expectedClassName")) {
                            mainLineClassName = className.internalName.replace('/', '.')
                        }
                        classLoader.addClass(className, it.bytes)
                    }

            val scriptClass = try {
                classLoader.loadClass(mainLineClassName!!)
            }
            catch (t: Throwable) {
                throw Exception("Error loading class $mainLineClassName: known classes: ${compiledClassesNames()}", t)
            }
            Pair(classLoader, scriptClass)
        }
    }

    override fun eval(compileResult: ReplCompileResponse.CompiledClasses,
                      scriptArgs: ScriptArgsWithTypes?,
                      invokeWrapper: InvokeWrapper?): ReplEvalResponse {
        stateLock.write {
            val verifyHistory = compileResult.compiledHistory.dropLast(1)
            val defaultHistoryActor = HistoryActions(
                    effectiveHistory = evaluatedHistory.copyValues(),
                    verify = { line -> evaluatedHistory.firstMismatchingHistory(line) },
                    addPlaceholder = { line, value -> evaluatedHistory.add(line, value) },
                    removePlaceholder = { line -> evaluatedHistory.removeLast(line) },
                    addFinal = { line, value -> evaluatedHistory.add(line, value) },
                    processClasses = { compiled ->
                        prependClassLoaderWithNewClasses(evaluatedHistory.copyValues(), compiled)
                    })

            val historyActor: HistoryActions = when (repeatingMode) {
                ReplRepeatingMode.NONE -> defaultHistoryActor
                ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT -> {
                    val lastItem = evaluatedHistory.lastItem()
                    if (lastItem == null || lastItem.first.source != compileResult.compiledCodeLine.source) {
                        defaultHistoryActor
                    }
                    else {
                        val trimmedHistory = ReplHistory(evaluatedHistory.copyAll().dropLast(1))
                        HistoryActions(
                                effectiveHistory = trimmedHistory.copyValues(),
                                verify = { trimmedHistory.firstMismatchingHistory(it) },
                                addPlaceholder = { _, _ -> NO_ACTION() },
                                removePlaceholder = { NO_ACTION_THAT_RETURNS(true) },
                                addFinal = { line, value ->
                                    evaluatedHistory.removeLast(line)
                                    evaluatedHistory.add(line, value)
                                },
                                processClasses = { _ ->
                                    Pair(lastItem.second.classLoader, lastItem.second.klass.java)
                                })
                    }
                }
                ReplRepeatingMode.REPEAT_ANY_PREVIOUS -> {
                    if (evaluatedHistory.isEmpty() || !evaluatedHistory.contains(compileResult.compiledCodeLine.source)) {
                        defaultHistoryActor
                    }
                    else {
                        val historyCopy = evaluatedHistory.copyAll()
                        val matchingItem = historyCopy.first { it.first.source == compileResult.compiledCodeLine.source }
                        val trimmedHistory = ReplHistory(evaluatedHistory.copyAll().takeWhile { it != matchingItem })
                        HistoryActions(
                                effectiveHistory = trimmedHistory.copyValues(),
                                verify = { trimmedHistory.firstMismatchingHistory(it) },
                                addPlaceholder = { _, _ -> NO_ACTION() },
                                removePlaceholder = { NO_ACTION_THAT_RETURNS(true) },
                                addFinal = { line, value ->
                                    val extraLines = evaluatedHistory.resetToLine(line)
                                    evaluatedHistory.removeLast(line)
                                    evaluatedHistory.add(line, value)
                                    extraLines.forEach {
                                        evaluatedHistory.add(it.first, it.second)
                                    }
                                },
                                processClasses = { _ ->
                                    Pair(matchingItem.second.classLoader, matchingItem.second.klass.java)
                                })
                    }
                }
            }

            val firstMismatch = historyActor.verify(verifyHistory)
            if (firstMismatch != null) {
                return@eval ReplEvalResponse.HistoryMismatch(evaluatedHistory.copySources(), firstMismatch)
            }

            val (classLoader, scriptClass) = try {
                historyActor.processClasses(compileResult)
            }
            catch (e: Exception) {
                return@eval ReplEvalResponse.Error.Runtime(evaluatedHistory.copySources(),
                                                           e.message!!, e)
            }

            val currentScriptArgs = scriptArgs ?: fallbackScriptArgs
            val useScriptArgs = currentScriptArgs?.scriptArgs
            val useScriptArgsTypes = currentScriptArgs?.scriptArgsTypes?.map { it.java }

            val constructorParams: Array<Class<*>> = (historyActor.effectiveHistory.map { it.klass.java } +
                                                      (useScriptArgs?.mapIndexed { i, it -> useScriptArgsTypes?.getOrNull(i) ?: it?.javaClass ?: Any::class.java } ?: emptyList())
                                                     ).toTypedArray()
            val constructorArgs: Array<Any?> = (historyActor.effectiveHistory.map { it.instance } + useScriptArgs.orEmpty()).toTypedArray()

            val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)

            historyActor.addPlaceholder(compileResult.compiledCodeLine, EvalClassWithInstanceAndLoader(scriptClass.kotlin, null, classLoader, invokeWrapper))

            val scriptInstance =
                    try {
                        if (invokeWrapper != null) invokeWrapper.invoke { scriptInstanceConstructor.newInstance(*constructorArgs) }
                        else scriptInstanceConstructor.newInstance(*constructorArgs)
                    }
                    catch (e: Throwable) {
                        historyActor.removePlaceholder(compileResult.compiledCodeLine)

                        // ignore everything in the stack trace until this constructor call
                        return@eval ReplEvalResponse.Error.Runtime(evaluatedHistory.copySources(),
                                                                   renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e as? Exception)
                    }

            historyActor.removePlaceholder(compileResult.compiledCodeLine)
            historyActor.addFinal(compileResult.compiledCodeLine, EvalClassWithInstanceAndLoader(scriptClass.kotlin, scriptInstance, classLoader, invokeWrapper))

            val resultField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
            val resultValue: Any? = resultField.get(scriptInstance)

            return if (compileResult.hasResult) ReplEvalResponse.ValueResult(evaluatedHistory.copySources(), resultValue)
            else ReplEvalResponse.UnitResult(evaluatedHistory.copySources())
        }
    }

    override val lastEvaluatedScripts: List<EvalHistoryType> get() {
        return stateLock.read { evaluatedHistory.copyAll() }
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }

    private fun makeReplClassLoader(baseClassloader: ClassLoader?, baseClasspath: Iterable<File>) =
            ReplClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))
}

