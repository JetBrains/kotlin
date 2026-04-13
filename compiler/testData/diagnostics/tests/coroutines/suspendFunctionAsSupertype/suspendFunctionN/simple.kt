// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT

import kotlin.coroutines.*

class C: SuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}

fun interface FI: SuspendFunction0<Unit> {
}

interface I: SuspendFunction0<Unit> {
}

object O: SuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, objectDeclaration,
operator, override, suspend */
