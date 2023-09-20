// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS

fun interface FISuper: () -> Unit

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, FISuper<!> {
    override suspend fun invoke() {
    }
}

fun interface FI: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, FISuper<!> {
}

interface I: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, FISuper<!> {
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, FISuper<!> {
    override suspend fun invoke() {
    }
}

fun interface SFISuper: suspend () -> Unit

class C1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, () -> Unit<!> {
    override suspend fun invoke() {
    }
}

fun interface FI1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, () -> Unit<!> {
}

interface I1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, () -> Unit<!> {
}

object O1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, () -> Unit<!> {
    override suspend fun invoke() {
    }
}

class C2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, FISuper<!> {
    override suspend fun invoke() {
    }
}

fun interface FI2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, FISuper<!> {
}

interface I2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, FISuper<!> {
}

object O2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SFISuper, FISuper<!> {
    override suspend fun invoke() {
    }
}
