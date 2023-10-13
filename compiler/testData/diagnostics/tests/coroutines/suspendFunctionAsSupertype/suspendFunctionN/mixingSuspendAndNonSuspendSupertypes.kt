// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS
import kotlin.coroutines.*

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SuspendFunction0<Unit>, () -> Unit<!> {
    override suspend fun invoke() {
    }
}

fun interface FI: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SuspendFunction0<Unit>, () -> Unit<!> {
}

interface I: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SuspendFunction0<Unit>, () -> Unit<!> {
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SuspendFunction0<Unit>, () -> Unit<!> {
    override suspend fun invoke() {
    }
}
