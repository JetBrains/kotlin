// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

class C: suspend () -> Unit, () -> Unit {
    override suspend fun invoke() {
    }
}

fun interface FI: suspend () -> Unit, () -> Unit {
}

interface I: suspend () -> Unit, () -> Unit {
}

object O: suspend () -> Unit, () -> Unit {
    override suspend fun invoke() {
    }
}