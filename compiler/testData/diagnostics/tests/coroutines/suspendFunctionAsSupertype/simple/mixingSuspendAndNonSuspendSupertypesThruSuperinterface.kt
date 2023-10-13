// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

interface ISuper: () -> Unit

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, ISuper<!> {
    override suspend fun invoke() {
    }
}

fun interface FI: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, ISuper<!> {
}

interface I: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, ISuper<!> {
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend () -> Unit, ISuper<!> {
    override suspend fun invoke() {
    }
}

interface SISuper: suspend () -> Unit

class C1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, () -> Unit<!> {
    override suspend fun invoke() {
    }
}

fun interface FI1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, () -> Unit<!> {
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

fun interface FI2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, ISuper<!> {
}

interface I2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, ISuper<!> {
}

object O2: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SISuper, ISuper<!> {
    override suspend fun invoke() {
    }
}
