// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

interface ISuper: () -> Unit

class C: suspend () -> Unit, ISuper {
    override suspend fun invoke() {
    }
}

fun interface FI: suspend () -> Unit, ISuper {
}

interface I: suspend () -> Unit, ISuper {
}

object O: suspend () -> Unit, ISuper {
    override suspend fun invoke() {
    }
}

interface SISuper: suspend () -> Unit

class C1: SISuper, () -> Unit {
    override suspend fun invoke() {
    }
}

fun interface FI1: SISuper, () -> Unit {
}

interface I1: SISuper, () -> Unit {
}

object O1: SISuper, () -> Unit {
    override suspend fun invoke() {
    }
}

class C2: SISuper, ISuper {
    override suspend fun invoke() {
    }
}

fun interface FI2: SISuper, ISuper {
}

interface I2: SISuper, ISuper {
}

object O2: SISuper, ISuper {
    override suspend fun invoke() {
    }
}