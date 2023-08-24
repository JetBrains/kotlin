// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, () -> Unit<!> {
    override suspend fun invoke() {
    }
}

interface I: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, () -> Unit<!> {
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, () -> Unit<!> {
    override suspend fun invoke() {
    }
}
