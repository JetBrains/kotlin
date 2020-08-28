/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.toState
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import kotlin.math.min

internal class ExceptionState private constructor(
    override val irClass: IrClass, override val fields: MutableList<Variable>, stackTrace: List<String>
) : Complex, Throwable() {

    override var superWrapperClass: Wrapper? = null
    override val typeArguments: MutableList<Variable> = mutableListOf()
    override var outerClass: Variable? = null

    override val message: String?
        get() = getState(messageProperty.symbol)?.asStringOrNull()
    override val cause: Throwable?
        get() = getState(causeProperty.symbol)?.let { if (it is ExceptionState) it else null }

    private lateinit var exceptionFqName: String
    private val exceptionHierarchy = mutableListOf<String>()
    private val messageProperty = irClass.getPropertyByName("message")
    private val causeProperty = irClass.getPropertyByName("cause")

    private val stackTrace: List<String> = stackTrace.reversed()

    init {
        if (!this::exceptionFqName.isInitialized) this.exceptionFqName = irClassFqName()

        if (fields.none { it.symbol == messageProperty.symbol }) {
            setMessage()
        }
    }

    constructor(common: Common, stackTrace: List<String>) : this(common.irClass, common.fields, stackTrace) {
        setUpCauseIfNeeded(common.superWrapperClass)
    }

    constructor(wrapper: Wrapper, stackTrace: List<String>) : this(wrapper.value as Throwable, wrapper.irClass, stackTrace) {
        setUpCauseIfNeeded(wrapper)
    }

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

    private fun setUpCauseIfNeeded(wrapper: Wrapper?) {
        val cause = (wrapper?.value as? Throwable)?.cause as? ExceptionState
        setCause(cause)
        if (message == null && cause != null) {
            val causeMessage = cause.exceptionFqName + (cause.message?.let { ": $it" } ?: "")
            setMessage(causeMessage)
        }
    }

    fun isSubtypeOf(ancestor: IrClass): Boolean {
        if (exceptionHierarchy.isNotEmpty()) {
            return exceptionHierarchy.any { it.contains(ancestor.name.asString()) }
        }
        return irClass.isSubclassOf(ancestor)
    }

    private fun setMessage(messageValue: String? = null) {
        setField(Variable(messageProperty.symbol, Primitive(messageValue, messageProperty.getter!!.returnType)))
    }

    private fun setCause(causeValue: State?) {
        setField(Variable(causeProperty.symbol, causeValue ?: Primitive<Throwable?>(null, causeProperty.getter!!.returnType)))
    }

    fun getFullDescription(): String {
        // TODO remainder of the stack trace with "..."
        val message = message.let { if (it?.isNotEmpty() == true) ": $it" else "" }
        val prefix = if (stackTrace.isNotEmpty()) "\n\t" else ""
        val postfix = if (stackTrace.size > 10) "\n\t..." else ""
        val causeMessage = (cause as? ExceptionState)?.getFullDescription()?.replaceFirst("Exception ", "\nCaused by: ") ?: ""
        return "Exception $exceptionFqName$message" +
                stackTrace.subList(0, min(stackTrace.size, 10)).joinToString(separator = "\n\t", prefix = prefix, postfix = postfix) +
                causeMessage
    }

    override fun toString(): String = message?.let { "$exceptionFqName: $it" } ?: exceptionFqName

    companion object {
        private fun IrClass.getPropertyByName(name: String): IrProperty {
            val property = this.declarations.single { it.nameForIrSerialization.asString() == name } as IrProperty
            return (property.getter!!.getLastOverridden() as IrSimpleFunction).correspondingPropertySymbol!!.owner
        }

        private fun evaluateFields(exception: Throwable, irClass: IrClass, stackTrace: List<String>): MutableList<Variable> {
            val messageProperty = irClass.getPropertyByName("message")
            val causeProperty = irClass.getPropertyByName("cause")

            val messageVar = Variable(messageProperty.symbol, exception.message.toState(messageProperty.getter!!.returnType))
            val causeVar = exception.cause?.let {
                Variable(causeProperty.symbol, ExceptionState(it, irClass, stackTrace + it.stackTrace.reversed().map { "at $it" }))
            }
            return listOfNotNull(messageVar, causeVar).toMutableList()
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