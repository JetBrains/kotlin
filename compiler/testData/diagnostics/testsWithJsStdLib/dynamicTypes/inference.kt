// FIR_IDENTICAL
// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t1: T, t2: T): T = t1

interface Tr
class C: Tr
fun <T: Tr> foo1(t1: T, t2: T): T = t1

fun test(d: dynamic, b: Boolean, n: String?) {
    foo(d, "").<!DEBUG_INFO_DYNAMIC!>foo<!>()
    foo1(d, C()).<!DEBUG_INFO_DYNAMIC!>foo<!>()

    val fromIf = if (b) d else ""
    fromIf.<!DEBUG_INFO_DYNAMIC!>doo<!>()

    val fromElvis = n ?: d
    fromElvis.<!DEBUG_INFO_DYNAMIC!>doo<!>()
}

class In<in T>(t: T)
fun <T> contra(a: In<T>, b: In<T>): T = null!!

fun testContra(d: dynamic) {
    contra(In(d), In("")).get(0) // not a dynamic call
}