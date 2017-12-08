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
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

open class GenericReplEvaluator(
        baseClasspath: List<File>,
        baseClassloader: ClassLoader? = Thread.currentThread().contextClassLoader,
        protected val fallbackScriptArgs: ScriptArgsWithTypes? = null,
        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT
) : ReplEvaluator {
    override val classLoader = ReplClassLoader(baseClasspath, baseClassloader)

    override fun createState(lock: ReentrantReadWriteLock) = GenericReplEvaluatorState(classLoader, lock)

    override fun eval(state: IReplStageState<*>,
                      compileResult: ReplCompileResult.CompiledClasses,
                      scriptArgs: ScriptArgsWithTypes?,
                      invokeWrapper: InvokeWrapper?): ReplEvalResult {
        state.lock.write {
            val evalState = state.asState<GenericReplEvaluatorState>()
            val historyActor = when (repeatingMode) {
                ReplRepeatingMode.NONE -> HistoryActionsForNoRepeat(evalState)
                ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT -> {
                    val lastItem = evalState.history.peek()
                    if (lastItem == null || lastItem.id != compileResult.lineId) {
                        HistoryActionsForNoRepeat(evalState)
                    }
                    else {
                        HistoryActionsForRepeatRecentOnly(evalState)
                    }
                }
                ReplRepeatingMode.REPEAT_ANY_PREVIOUS -> {
                    val matchingItem = evalState.history.firstOrNull { it.id == compileResult.lineId }
                    if (matchingItem == null) {
                        HistoryActionsForNoRepeat(evalState)
                    }
                    else {
                        HistoryActionsForRepeatAny(evalState, matchingItem)
                    }
                }
            }

            val firstMismatch = historyActor.firstMismatch(compileResult.previousLines.asSequence())
            if (firstMismatch != null) {
                return@eval ReplEvalResult.HistoryMismatch(firstMismatch.first?.id?.no ?: firstMismatch.second?.no ?: -1 /* means error? */)
            }

            val scriptClass = try {
                historyActor.processClasses(compileResult)
            }
            catch (e: Exception) {
                return@eval ReplEvalResult.Error.Runtime(e.message ?: "unknown", e)
            }

            historyActor.addPlaceholder(
                    compileResult.lineId,
                    EvalClassWithInstanceAndLoader(scriptClass.kotlin, null, evalState.classLoader, invokeWrapper))

            fun makeErrorMessage(e: Throwable) = renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.run")

            val scriptInstance =
                    try {
                        val scriptInstance = if (invokeWrapper != null) {
                            invokeWrapper.invoke { scriptClass.newInstance() }
                        }
                        else {
                            scriptClass.newInstance()
                        }

                        scriptInstance.javaClass.getDeclaredMethod("run").invoke(null)
                        scriptInstance
                    }
                    catch (e: InvocationTargetException) {
                        // ignore everything in the stack trace until this constructor call
                        return@eval ReplEvalResult.Error.Runtime(makeErrorMessage(e), e.targetException as? Exception)
                    }
                    catch (e: Throwable) {
                        // ignore everything in the stack trace until this constructor call
                        return@eval ReplEvalResult.Error.Runtime(makeErrorMessage(e), e as? Exception)
                    }
                    finally {
                        historyActor.removePlaceholder(compileResult.lineId)
                    }

            historyActor.addFinal(
                    compileResult.lineId,
                    EvalClassWithInstanceAndLoader(scriptClass.kotlin, scriptInstance, evalState.classLoader, invokeWrapper))

            val resultField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
            val resultValue: Any? = resultField.get(scriptInstance)

            return if (compileResult.hasResult) ReplEvalResult.ValueResult(resultValue, compileResult.type)
            else ReplEvalResult.UnitResult()
        }
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }

}

private open class HistoryActionsForNoRepeat(val state: GenericReplEvaluatorState) {
    open val effectiveHistory: List<EvalClassWithInstanceAndLoader>
        get() = state.history.map { it.item }

    open fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? {
        return state.history.firstMismatch(other)
    }

    open fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {
        state.history.push(lineId, value)
    }

    open fun removePlaceholder(lineId: ILineId): Boolean = state.history.verifiedPop(lineId) != null

    open fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {
        state.history.push(lineId, value)
    }

    open fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Class<out Any> {
        class ClassToLoad(val name: JvmClassName, val bytes: ByteArray)

        val classesToLoad = compileResult.classes
                .filter { it.path.endsWith(".class") }
                .map { ClassToLoad(JvmClassName.byInternalName(it.path.removeSuffix(".class")), it.bytes) }

        val mainClassName = compileResult.mainClassName

        if (!classesToLoad.any { it.name.internalName == mainClassName }) {
            val compiledClassNames = classesToLoad.joinToString { it.name.internalName }
            throw IllegalStateException("Error loading class $mainClassName: known classes: $compiledClassNames")
        }

        classesToLoad.forEach { state.classLoader.addClass(it.name, it.bytes) }
        return Class.forName(mainClassName, true, state.classLoader)
    }
}

private open class HistoryActionsForRepeatRecentOnly(state: GenericReplEvaluatorState) : HistoryActionsForNoRepeat(state) {

    val currentLast = state.history.peek()!!

    override val effectiveHistory: List<EvalClassWithInstanceAndLoader> get() = super.effectiveHistory.dropLast(1)

    override fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? =
            state.history.firstMismatchFiltered(other) { it.id != currentLast.id }

    override fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {}

    override fun removePlaceholder(lineId: ILineId): Boolean = true

    override fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {
        state.history.pop()
        state.history.push(lineId, value)
    }

    override fun processClasses(compileResult: ReplCompileResult.CompiledClasses) = currentLast.item.klass.java
}

private open class HistoryActionsForRepeatAny(state: GenericReplEvaluatorState, val matchingLine: ReplHistoryRecord<EvalClassWithInstanceAndLoader>): HistoryActionsForNoRepeat(state) {

    override val effectiveHistory: List<EvalClassWithInstanceAndLoader> get() = state.history.takeWhile { it.id != matchingLine.id }.map { it.item }

    override fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? =
            state.history.firstMismatchWhile(other) { it.id != matchingLine.id }

    override fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {}

    override fun removePlaceholder(lineId: ILineId): Boolean = true

    override fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {
        val extraLines = state.history.takeLastWhile { it.id == matchingLine.id }
        state.history.resetTo(lineId)
        state.history.pop()
        state.history.push(lineId, value)
        extraLines.forEach {
            state.history.push(it.id, it.item)
        }
    }

    override fun processClasses(compileResult: ReplCompileResult.CompiledClasses) = matchingLine.item.klass.java
}
