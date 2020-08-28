// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1

fun test0(serialize: KSuspendFunction0<Unit>) {}
fun test1(serialize: KSuspendFunction1<Int, Unit>) {}

suspend fun foo() {}
suspend fun bar(x: Int) {}

fun test() {
    test0(::foo)
    <!INAPPLICABLE_CANDIDATE!>test1<!>(<!UNRESOLVED_REFERENCE!>::foo<!>)

    <!INAPPLICABLE_CANDIDATE!>test0<!>(<!UNRESOLVED_REFERENCE!>::bar<!>)
    test1(::bar)
}
