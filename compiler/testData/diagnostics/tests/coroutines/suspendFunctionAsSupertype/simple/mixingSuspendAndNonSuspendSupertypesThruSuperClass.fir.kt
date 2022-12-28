// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS

abstract class CSuper: () -> Unit

class C: CSuper(), suspend () -> Unit {
    override suspend fun invoke() {
    }
}

object O: CSuper(), suspend () -> Unit {
    override suspend fun invoke() {
    }
}

abstract class SCSuper: suspend () -> Unit

class C1: SCSuper(), () -> Unit {
    override suspend fun invoke() {
    }
}

object O1: SCSuper(), () -> Unit {
    override suspend fun invoke() {
    }
}