/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.`when`

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

abstract class SwitchCodegen(
    @JvmField
    protected val expression: KtWhenExpression,
    protected val isStatement: Boolean,
    private val isExhaustive: Boolean,
    @JvmField
    protected val codegen: ExpressionCodegen,
    subjectType: Type?
) {
    protected val bindingContext: BindingContext = codegen.bindingContext

    @JvmField
    protected val subjectType = subjectType ?: codegen.expressionType(expression.subjectExpression)

    protected val resultType: Type = if (isStatement) Type.VOID_TYPE else codegen.expressionType(expression)

    @JvmField
    protected val v: InstructionAdapter = codegen.v

    @JvmField
    protected val transitionsTable: NavigableMap<Int, Label> = TreeMap()

    private val entryLabels: MutableList<Label> = ArrayList()
    private var elseLabel = Label()
    private var endLabel = Label()
    protected lateinit var defaultLabel: Label

    private val switchCodegenProvider = SwitchCodegenProvider(codegen)

    /**
     * Generates bytecode for entire when expression
     */
    open fun generate() {
        prepareConfiguration()

        val hasElse = expression.elseExpression != null

        // if there is no else-entry and it's statement then default --- endLabel
        defaultLabel = if (hasElse || !isStatement || isExhaustive) elseLabel else endLabel

        generateSubject()

        generateSwitchInstructionByTransitionsTable()

        generateEntries()

        // there is no else-entry but this is not statement, so we should return Unit
        if (!hasElse && (!isStatement || isExhaustive)) {
            v.visitLabel(elseLabel)
            codegen.putUnitInstanceOntoStackForNonExhaustiveWhen(expression, isStatement)
        }

        codegen.markLineNumber(expression, isStatement)
        v.mark(endLabel)
    }

    /**
     * Sets up transitionsTable and maybe something else needed in a special case
     * Behaviour may be changed by overriding processConstant
     */
    private fun prepareConfiguration() {
        for (entry in expression.entries) {
            val entryLabel = Label()

            for (constant in switchCodegenProvider.getConstantsFromEntry(entry)) {
                if (constant is NullValue || constant == null) continue
                processConstant(constant, entryLabel)
            }

            if (entry.isElse) {
                elseLabel = entryLabel
            }

            entryLabels.add(entryLabel)
        }
    }

    protected abstract fun processConstant(
        constant: ConstantValue<*>,
        entryLabel: Label
    )

    protected fun putTransitionOnce(value: Int, entryLabel: Label) {
        if (!transitionsTable.containsKey(value)) {
            transitionsTable[value] = entryLabel
        }
    }

    /**
     * Should generate int subject on top of the stack
     * Default implementation just run codegen for actual subject of expression
     * May also gen nullability check if needed
     */
    protected open fun generateSubject() {
        codegen.gen(expression.subjectExpression, subjectType)
    }

    protected fun generateNullCheckIfNeeded() {
        assert(expression.subjectExpression != null) { "subject expression can't be null" }
        val subjectJetType = bindingContext.getType(expression.subjectExpression!!) ?: error("subject type can't be null (i.e. void)")

        if (TypeUtils.isNullableType(subjectJetType)) {
            val nullEntryIndex = findNullEntryIndex(expression)
            val nullLabel = if (nullEntryIndex == -1) defaultLabel else entryLabels[nullEntryIndex]
            val notNullLabel = Label()

            with(v) {
                dup()
                ifnonnull(notNullLabel)
                pop()
                goTo(nullLabel)
                visitLabel(notNullLabel)
            }
        }
    }

    private fun findNullEntryIndex(expression: KtWhenExpression) =
        expression.entries.withIndex().firstOrNull { (_, entry) ->
            switchCodegenProvider.getConstantsFromEntry(entry).any { it is NullValue }
        }?.index ?: -1

    private fun generateSwitchInstructionByTransitionsTable() {
        val keys = transitionsTable.keys.toIntArray()

        val labelsNumber = keys.size
        val maxValue = keys.last()
        val minValue = keys.first()
        val rangeLength = maxValue.toLong() - minValue.toLong() + 1L

        // In modern JVM implementations it shouldn't matter very much for runtime performance
        // whether to choose lookupswitch or tableswitch.
        // The only metric that really matters is bytecode size and here we can estimate:
        // - lookupswitch: ~ 2 * labelsNumber
        // - tableswitch: ~ rangeLength
        if (rangeLength > 2L * labelsNumber || rangeLength > Int.MAX_VALUE) {
            val labels = transitionsTable.values.toTypedArray()
            v.lookupswitch(defaultLabel, keys, labels)
            return
        }

        val sparseLabels = Array(rangeLength.toInt()) { index ->
            transitionsTable[index + minValue] ?: defaultLabel
        }

        v.tableswitch(minValue, maxValue, defaultLabel, *sparseLabels)
    }

    protected open fun generateEntries() {
        // resolving entries' entryLabels and generating entries' code
        val entryLabelsIterator = entryLabels.iterator()
        for (entry in expression.entries) {
            v.visitLabel(entryLabelsIterator.next())

            val mark = codegen.myFrameMap.mark()
            codegen.gen(entry.expression, resultType)
            mark.dropTo()

            if (!entry.isElse) {
                v.goTo(endLabel)
            }
        }
    }
}
