// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

import kotlin.coroutines.*

interface ISuper: () -> Unit

class C: SuspendFunction0<Unit>, ISuper {
    override suspend fun invoke() {
    }
}

fun interface FI: SuspendFunction0<Unit>, ISuper {
}

interface I: SuspendFunction0<Unit>, ISuper {
}

object O: SuspendFunction0<Unit>, ISuper {
    override suspend fun invoke() {
    }
}

interface SISuper: SuspendFunction0<Unit>

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