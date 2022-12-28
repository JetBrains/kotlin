// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// FIR_IDENTICAL

class C: suspend () -> Unit {
    override suspend fun invoke() {
    }
}

fun interface FI: suspend () -> Unit {
}

interface I: suspend () -> Unit {
}

object O: suspend () -> Unit {
    override suspend fun invoke() {
    }
}
