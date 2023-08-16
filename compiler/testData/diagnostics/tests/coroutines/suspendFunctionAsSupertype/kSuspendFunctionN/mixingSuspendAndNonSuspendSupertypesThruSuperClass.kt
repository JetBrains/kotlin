// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

abstract class CSuper: () -> Unit

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>CSuper(), KSuspendFunction0<Unit><!> {
    override suspend fun invoke() {
    }
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>CSuper(), KSuspendFunction0<Unit><!> {
    override suspend fun invoke() {
    }
}

abstract class SCSuper: KSuspendFunction0<Unit>

class C1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SCSuper(), () -> Unit<!> {
    override suspend fun invoke() {
    }
}

object O1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SCSuper(), () -> Unit<!> {
    override suspend fun invoke() {
    }
}
