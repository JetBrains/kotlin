// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS
import kotlin.coroutines.*

class C: SuspendFunction0<Unit>, () -> Unit {
    override suspend fun invoke() {
    }
}

fun interface FI: SuspendFunction0<Unit>, () -> Unit {
}

interface I: SuspendFunction0<Unit>, () -> Unit {
}

object O: SuspendFunction0<Unit>, () -> Unit {
    override suspend fun invoke() {
    }
}