// !DIAGNOSTICS: -UNUSED_PARAMETER
// !OPT_IN: kotlin.RequiresOptIn

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
    test1(::<!INAPPLICABLE_CANDIDATE!>foo<!>)

    test0(::<!INAPPLICABLE_CANDIDATE!>bar<!>)
    test1(::bar)
}
