/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.openapi.project.Project
import com.sun.jdi.ObjectReference
import com.sun.jdi.Value
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ContinuationHolder

class ContinuationValueDescriptorImpl(
    project: Project,
    val continuation: ObjectReference,
    val fieldName: String,
    val variableName: String
) : ValueDescriptorImpl(project) {
    override fun calcValueName() = variableName

    override fun calcValue(evaluationContext: EvaluationContextImpl?): Value? {
        val field = continuation.referenceType()?.fieldByName(fieldName) ?: return null
        return continuation.getValue(field)
    }

    override fun getDescriptorEvaluation(context: DebuggerContext?) =
        throw EvaluateException("Spilled variable evaluation is not supported")
}