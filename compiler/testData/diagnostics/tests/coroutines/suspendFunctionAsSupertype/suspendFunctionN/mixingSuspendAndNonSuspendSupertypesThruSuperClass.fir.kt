// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

import kotlin.coroutines.*

abstract class CSuper: () -> Unit

class C: CSuper(), SuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}

object O: CSuper(), SuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}

abstract class SCSuper: SuspendFunction0<Unit>

class C1: SCSuper(), () -> Unit {
    override suspend fun invoke() {
    }
}

object O1: SCSuper(), () -> Unit {
    override suspend fun invoke() {
    }
}