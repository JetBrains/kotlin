// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

abstract class CSuper: () -> Unit

class C: CSuper(), KSuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}

object O: CSuper(), KSuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}

abstract class SCSuper: KSuspendFunction0<Unit>

class C1: SCSuper(), () -> Unit {
    override suspend fun invoke() {
    }
}

object O1: SCSuper(), () -> Unit {
    override suspend fun invoke() {
    }
}