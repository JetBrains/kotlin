// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

import kotlin.coroutines.*

fun interface FISuper: () -> Unit

class C: SuspendFunction0<Unit>, FISuper {
    override suspend fun invoke() {
    }
}

fun interface FI: SuspendFunction0<Unit>, FISuper {
}

interface I: SuspendFunction0<Unit>, FISuper {
}

object O: SuspendFunction0<Unit>, FISuper {
    override suspend fun invoke() {
    }
}

fun interface SFISuper: SuspendFunction0<Unit>

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