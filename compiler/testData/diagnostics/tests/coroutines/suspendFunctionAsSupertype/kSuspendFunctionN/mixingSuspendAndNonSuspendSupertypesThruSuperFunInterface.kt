// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

fun interface FISuper: () -> Unit

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, FISuper<!> {
    override suspend fun invoke() {
    }
}

interface I: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, FISuper<!> {
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, FISuper<!> {
    override suspend fun invoke() {
    }
}
