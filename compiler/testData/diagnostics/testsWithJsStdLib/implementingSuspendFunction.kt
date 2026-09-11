// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -ABSTRACT_MEMBER_NOT_IMPLEMENTED

import kotlin.coroutines.SuspendFunction1
import kotlin.reflect.KSuspendFunction1

abstract class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>C<!> : suspend () -> Unit {
    override suspend fun invoke() {}
}

interface <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>I<!> : suspend () -> Unit

fun interface <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>FI<!> : suspend () -> Unit

<!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>object O<!> : suspend (String) -> Int {
    override suspend fun invoke(value: String): Int = value.length
}

fun anonymousObject(): Any = <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>object<!> : suspend () -> Unit {
    override suspend fun invoke() {}
}

interface <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>I2<!> : suspend (Int) -> Unit, suspend (Int, Int) -> Unit

abstract class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>D<!> : C()

abstract class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>ExplicitSuspend<!> : SuspendFunction1<Int, Int>

abstract class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>ExplicitKSuspend<!> : KSuspendFunction1<Int, Int>

abstract class <!IMPLEMENTING_FUNCTION_INTERFACE, IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>Mixed<!> : <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>suspend (Int) -> Unit, (Int, Int) -> Unit<!>

class Plain

class Uses(val function: suspend () -> Unit)