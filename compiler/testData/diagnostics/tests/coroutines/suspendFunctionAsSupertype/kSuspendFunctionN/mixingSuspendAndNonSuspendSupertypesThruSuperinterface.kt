// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

interface ISuper: () -> Unit

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, ISuper<!> {
    override suspend fun invoke() {
    }
}

interface I: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, ISuper<!> {
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, ISuper<!> {
    override suspend fun invoke() {
    }
}

interface SISuper: KSuspendFunction0<Unit>

class C1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, () -> Unit<!> {
    override suspend fun invoke() {
    }
}

interface I1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, () -> Unit<!> {
}

object O1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, () -> Unit<!> {
    override suspend fun invoke() {
    }
}

class C2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, ISuper<!> {
    override suspend fun invoke() {
    }
}

interface I2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, ISuper<!> {
}

object O2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, ISuper<!> {
    override suspend fun invoke() {
    }
}
