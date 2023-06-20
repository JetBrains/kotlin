// ORIGINAL: /compiler/testData/diagnostics/tests/coroutines/suspendFunctionAsSupertype/kSuspendFunctionN/mixingSuspendAndNonSuspendSupertypes.fir.kt
// WITH_STDLIB
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

class C: KSuspendFunction0<Unit>, () -> Unit {
    override suspend fun invoke() {
    }
}

interface I: KSuspendFunction0<Unit>, () -> Unit {
}

object O: KSuspendFunction0<Unit>, () -> Unit {
    override suspend fun invoke() {
    }
}

fun box() = "OK"
