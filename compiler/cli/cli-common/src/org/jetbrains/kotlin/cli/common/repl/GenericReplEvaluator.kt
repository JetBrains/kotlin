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
        val baseClasspath: Iterable<File>,
        val baseClassloader: ClassLoader? = Thread.currentThread().contextClassLoader,
        protected val fallbackScriptArgs: ScriptArgsWithTypes? = null,
        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT
) : ReplEvaluator {

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = GenericReplEvaluatorState(baseClasspath, baseClassloader, lock)

    override fun eval(state: IReplStageState<*>,
                      compileResult: ReplCompileResult.CompiledClasses,
                      scriptArgs: ScriptArgsWithTypes?,
                      invokeWrapper: InvokeWrapper?): ReplEvalResult {
        state.lock.write {
            val evalState = state.asState(GenericReplEvaluatorState::class.java)
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

            val (classLoader, scriptClass) = try {
                historyActor.processClasses(compileResult)
            }
            catch (e: Exception) {
                return@eval ReplEvalResult.Error.Runtime(e.message ?: "unknown", e)
            }

            val currentScriptArgs = scriptArgs ?: fallbackScriptArgs
            val useScriptArgs = currentScriptArgs?.scriptArgs
            val useScriptArgsTypes = currentScriptArgs?.scriptArgsTypes?.map { it.java }

            val hasHistory = historyActor.effectiveHistory.isNotEmpty()

            val constructorParams: Array<Class<*>> = (if (hasHistory) arrayOf<Class<*>>(Array<Any>::class.java) else emptyArray<Class<*>>()) +
                                                     (useScriptArgs?.mapIndexed { i, it -> useScriptArgsTypes?.getOrNull(i) ?: it?.javaClass ?: Any::class.java } ?: emptyList())

            val constructorArgs: Array<out Any?> = if (hasHistory) arrayOf(historyActor.effectiveHistory.map { it.instance }.takeIf { it.isNotEmpty() }?.toTypedArray(),
                                                                           *(useScriptArgs.orEmpty()))
                                                   else useScriptArgs.orEmpty()

            // TODO: try/catch ?
            val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)

            historyActor.addPlaceholder(compileResult.lineId, EvalClassWithInstanceAndLoader(scriptClass.kotlin, null, classLoader, invokeWrapper))

            val scriptInstance =
                    try {
                        if (invokeWrapper != null) invokeWrapper.invoke { scriptInstanceConstructor.newInstance(*constructorArgs) }
                        else scriptInstanceConstructor.newInstance(*constructorArgs)
                    }
                    catch (e: InvocationTargetException) {
                        // ignore everything in the stack trace until this constructor call
                        return@eval ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e.targetException as? Exception)
                    }
                    catch (e: Throwable) {
                        // ignore everything in the stack trace until this constructor call
                        return@eval ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e as? Exception)
                    }
                    finally {
                        historyActor.removePlaceholder(compileResult.lineId)
                    }

            historyActor.addFinal(compileResult.lineId, EvalClassWithInstanceAndLoader(scriptClass.kotlin, scriptInstance, classLoader, invokeWrapper))

            val resultFieldName = scriptResultFieldName(compileResult.lineId.no)
            val resultField = scriptClass.getDeclaredField(resultFieldName).apply { isAccessible = true }
            val resultValue: Any? = resultField.get(scriptInstance)

            return if (compileResult.hasResult) ReplEvalResult.ValueResult(resultFieldName, resultValue, compileResult.type)
            else ReplEvalResult.UnitResult()
        }
    }
}

private open class HistoryActionsForNoRepeat(val state: GenericReplEvaluatorState) {

    open val effectiveHistory: List<EvalClassWithInstanceAndLoader> get() = state.history.map { it.item }

    open fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? = state.history.firstMismatch(other)

    open fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) { state.history.push(lineId, value) }

    open fun removePlaceholder(lineId: ILineId): Boolean = state.history.verifiedPop(lineId) != null

    open fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) { state.history.push(lineId, value) }

    open fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Pair<ClassLoader, Class<out Any>> = prependClassLoaderWithNewClasses(effectiveHistory, compileResult)

    private fun prependClassLoaderWithNewClasses(effectiveHistory: List<EvalClassWithInstanceAndLoader>,
                                                 compileResult: ReplCompileResult.CompiledClasses
    ): Pair<ClassLoader, Class<out Any>> {
        var mainLineClassName: String? = null
        val classLoader = makeReplClassLoader(effectiveHistory.lastOrNull()?.classLoader ?: state.topClassLoader, compileResult.classpathAddendum)
        fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.removeSuffix(".class"))
        fun compiledClassesNames() = compileResult.classes.map { classNameFromPath(it.path).internalName.replace('/', '.') }
        val expectedClassName = compileResult.mainClassName
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
        return Pair(classLoader, scriptClass)
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

    override fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Pair<ClassLoader, Class<out Any>> =
            currentLast.item.classLoader to currentLast.item.klass.java
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

    override fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Pair<ClassLoader, Class<out Any>> =
            matchingLine.item.classLoader to matchingLine.item.klass.java
}
