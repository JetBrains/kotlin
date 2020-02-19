// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// ISSUE: KT-36819

fun <K> select(vararg x: K) = x[0]
interface A
class B: A
class C: A
fun <T> id1(x: T): T = x
fun <R> id2(x: R): R = x

class Out<out R>(x: R)

fun main() {
    val x1 = select(id1 { B() }, id2 { C() })
    val x2 = select({ B() }, { C() }) // OK, CST = () -> A
    val x3 = select(id1(Out(B())), id2(Out(C()))) // OK, CST = Out<A>
}
