// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// FIR_IDENTICAL

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
