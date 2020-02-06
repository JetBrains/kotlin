/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger

class EvaluationStatus {
    private var error: EvaluationError? = null
    private var values = mutableMapOf<String, String>()
    private var evaluationTypeSet: Boolean = false

    fun error(kind: EvaluationError) {
        if (error == null) {
            error = kind
        }
    }

    fun flag(name: String, value: Boolean) {
        values[name] = value.toString()
    }

    fun <T : Enum<T>> value(name: String, value: T) {
        values[name] = value.name
    }

    fun evaluationType(type: KotlinDebuggerEvaluator.EvaluationType) {
        evaluationTypeSet = true
        value("evaluationType", type)
    }

    fun send() {
        if (!evaluationTypeSet) {
            return
        }

        values["status"] = error?.name ?: "Success"
        KotlinFUSLogger.log(FUSEventGroups.Debug, "Evaluation", values)
    }

    enum class EvaluatorType {
        Bytecode, Eval4j
    }

    enum class EvaluationContextLanguage {
        Java, Kotlin, Other
    }
}

enum class EvaluationError {
    DebuggerNotAttached,
    DumbMode,
    NoFrameProxy,
    ThreadNotAvailable,
    ThreadNotSuspended,

    ProcessCancelledException,
    InterpretingException,
    EvaluateException,
    SpecialException,
    GenericException,
    CannotFindVariable,

    CoroutineContextUnavailable,
    ParameterNotCaptured,
    InsideDefaultMethod,
    BackingFieldNotFound,
    SuspendCall,
    CrossInlineLambda,

    Eval4JAbnormalTermination,
    Eval4JUnknownException,

    ExceptionFromEvaluatedCode,
    ErrorElementOccurred,
    FrontendException,
    BackendException,
    ErrorsInCode
}

fun EvaluationStatus.classLoadingFailed() {
    flag("classLoadingFailed", true)
}

fun EvaluationStatus.compilingEvaluatorFailed() {
    flag("compilingEvaluatorFailed", true)
}

fun EvaluationStatus.usedEvaluator(evaluator: EvaluationStatus.EvaluatorType) {
    value("evaluator", evaluator)
}

fun EvaluationStatus.contextLanguage(language: EvaluationStatus.EvaluationContextLanguage) {
    value("contextLanguage", language)
}