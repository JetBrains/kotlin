// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

fun interface FISuper: () -> Unit

class C: suspend () -> Unit, FISuper {
    override suspend fun invoke() {
    }
}

fun interface FI: suspend () -> Unit, FISuper {
}

interface I: suspend () -> Unit, FISuper {
}

object O: suspend () -> Unit, FISuper {
    override suspend fun invoke() {
    }
}

fun interface SFISuper: suspend () -> Unit

class C1: SFISuper, () -> Unit {
    override suspend fun invoke() {
    }
}

fun interface FI1: SFISuper, () -> Unit {
}

interface I1: SFISuper, () -> Unit {
}

object O1: SFISuper, () -> Unit {
    override suspend fun invoke() {
    }
}

class C2: SFISuper, FISuper {
    override suspend fun invoke() {
    }
}

fun interface FI2: SFISuper, FISuper {
}

interface I2: SFISuper, FISuper {
}

object O2: SFISuper, FISuper {
    override suspend fun invoke() {
    }
}
