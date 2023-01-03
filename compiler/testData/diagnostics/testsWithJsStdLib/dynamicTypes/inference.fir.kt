// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t1: T, t2: T): T = t1

interface Tr
class C: Tr
fun <T: Tr> foo1(t1: T, t2: T): T = t1

fun test(d: dynamic, b: Boolean, n: String?) {
    foo(d, "").foo()
    foo1(d, C()).foo()

    val fromIf = if (b) d else ""
    fromIf.doo()

    val fromElvis = n ?: d
    fromElvis.doo()
}

class In<in T>(t: T)
fun <T> contra(a: In<T>, b: In<T>): T = null!!

fun testContra(d: dynamic) {
    contra(In(d), In("")).get(0) // not a dynamic call
}
