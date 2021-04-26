// FIR_IDENTICAL
// !LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// FIR_IDENTICAL
// DIAGNOSTICS: -ABSTRACT_MEMBER_NOT_IMPLEMENTED

import kotlin.reflect.*

class C: KSuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}

interface I: KSuspendFunction0<Unit> {
}

object O: KSuspendFunction0<Unit> {
    override suspend fun invoke() {
    }
}
