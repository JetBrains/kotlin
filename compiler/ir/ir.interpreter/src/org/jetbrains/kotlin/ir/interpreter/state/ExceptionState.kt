/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.interpreter.getOriginalPropertyByName
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.toState
import org.jetbrains.kotlin.ir.util.isSubclassOf
import kotlin.math.min

internal class ExceptionState private constructor(
    override val irClass: IrClass, override val fields: MutableList<Variable>, stackTrace: List<String>
) : Complex, StateWithClosure, Throwable() {
    override val upValues: MutableList<Variable> = mutableListOf()
    override var superWrapperClass: Wrapper? = null
    override var outerClass: Variable? = null

    override val message: String?
        get() = getField(messageProperty.symbol)?.asStringOrNull()
    override val cause: ExceptionState?
        get() = getField(causeProperty.symbol) as? ExceptionState

    private lateinit var exceptionFqName: String
    private val exceptionHierarchy = mutableListOf<String>()
    private val messageProperty = irClass.getOriginalPropertyByName("message")
    private val causeProperty = irClass.getOriginalPropertyByName("cause")

    private val stackTrace: List<String> = stackTrace.reversed()

    init {
        if (!this::exceptionFqName.isInitialized) this.exceptionFqName = irClassFqName()
        if (fields.none { it.symbol == messageProperty.symbol }) setMessage(null)
        if (fields.none { it.symbol == causeProperty.symbol }) setCause(null)
    }

    constructor(irClass: IrClass, stackTrace: List<String>) : this(irClass, mutableListOf(), stackTrace)

    constructor(
        exception: Throwable, irClass: IrClass, stackTrace: List<String>
    ) : this(irClass, evaluateFields(exception, irClass, stackTrace), stackTrace + evaluateAdditionalStackTrace(exception)) {
        setCause(null)  // TODO check this fact
        if (irClass.name.asString() != exception::class.java.simpleName) {
            // ir class wasn't found in classpath, a stub was passed => need to save java class hierarchy
            this.exceptionFqName = exception::class.java.name
            exceptionHierarchy += this.exceptionFqName
            generateSequence(exception::class.java.superclass) { it.superclass }.forEach { exceptionHierarchy += it.name }
            exceptionHierarchy.removeAt(exceptionHierarchy.lastIndex) // remove unnecessary java.lang.Object
        }
    }

    fun copyFieldsFrom(wrapper: Wrapper) {
        (wrapper.value as? Throwable)?.let {
            setMessage(it.message)
            setCause(it.cause as? ExceptionState)
        }
    }

    override fun setField(newVar: Variable) {
        super.setField(newVar)
        recalculateCauseAndMessage()
    }

    private fun recalculateCauseAndMessage() {
        if (message == null && cause != null) {
            val causeMessage = cause!!.exceptionFqName + (cause!!.message?.let { ": $it" } ?: "")
            setMessage(causeMessage)
        }
    }

    fun isSubtypeOf(ancestor: IrClass): Boolean {
        if (exceptionHierarchy.isNotEmpty()) {
            return exceptionHierarchy.any { it.contains(ancestor.name.asString()) }
        }
        return irClass.isSubclassOf(ancestor)
    }

    private fun setMessage(messageValue: String?) {
        setField(Variable(messageProperty.symbol, Primitive(messageValue, messageProperty.getter!!.returnType)))
    }

    private fun setCause(causeValue: State?) {
        setField(Variable(causeProperty.symbol, causeValue ?: Primitive.nullStateOfType(causeProperty.getter!!.returnType)))
    }

    fun getFullDescription(): String {
        // TODO remainder of the stack trace with "..."
        val message = message.let { if (it?.isNotEmpty() == true) ": $it" else "" }
        val prefix = if (stackTrace.isNotEmpty()) "\n\t" else ""
        val postfix = if (stackTrace.size > 10) "\n\t..." else ""
        val causeMessage = cause?.getFullDescription()?.replaceFirst("Exception ", "\nCaused by: ") ?: ""
        return "Exception $exceptionFqName$message" +
                stackTrace.subList(0, min(stackTrace.size, 10)).joinToString(separator = "\n\t", prefix = prefix, postfix = postfix) +
                causeMessage
    }

    override fun toString(): String = message?.let { "$exceptionFqName: $it" } ?: exceptionFqName

    companion object {
        private fun evaluateFields(exception: Throwable, irClass: IrClass, stackTrace: List<String>): MutableList<Variable> {
            val messageProperty = irClass.getOriginalPropertyByName("message")
            val causeProperty = irClass.getOriginalPropertyByName("cause")

            val messageVar = Variable(messageProperty.symbol, exception.message.toState(messageProperty.getter!!.returnType))
            val causeVar = exception.cause?.let {
                Variable(causeProperty.symbol, ExceptionState(it, irClass, stackTrace + it.stackTrace.reversed().map { "at $it" }))
            }
            return causeVar?.let { mutableListOf(messageVar, it) } ?: mutableListOf(messageVar)
        }

        private fun evaluateAdditionalStackTrace(e: Throwable): List<String> {
            // TODO do we really need this?... It will point to JVM stdlib
            val additionalStack = mutableListOf<String>()
            if (e.stackTrace.any { it.className == "java.lang.invoke.MethodHandle" }) {
                for ((index, stackTraceElement) in e.stackTrace.withIndex()) {
                    if (stackTraceElement.methodName == "invokeWithArguments") {
                        additionalStack.addAll(e.stackTrace.slice(0 until index).reversed().map { "at $it" })
                        break
                    }
                }

                var cause = e.cause
                val lastNeededValue = e.stackTrace.first().let { it.className + "." + it.methodName }
                while (cause != null) {
                    for ((causeStackIndex, causeStackTraceElement) in cause.stackTrace.withIndex()) {
                        val currentStackTraceValue = causeStackTraceElement.let { it.className + "." + it.methodName }
                        if (currentStackTraceValue == lastNeededValue) {
                            cause.stackTrace = cause.stackTrace.sliceArray(0 until causeStackIndex).reversedArray()
                            break
                        }
                    }
                    cause = cause.cause
                }
            }
            return additionalStack
        }
    }
}