// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

interface ISuper: () -> Unit

class C: KSuspendFunction0<Unit>, ISuper {
    override suspend fun invoke() {
    }
}

interface I: KSuspendFunction0<Unit>, ISuper {
}

object O: KSuspendFunction0<Unit>, ISuper {
    override suspend fun invoke() {
    }
}

interface SISuper: KSuspendFunction0<Unit>

class C1: SISuper, () -> Unit {
    override suspend fun invoke() {
    }
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

interface I2: SISuper, ISuper {
}

object O2: SISuper, ISuper {
    override suspend fun invoke() {
    }
}