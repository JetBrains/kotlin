// DUMP_IR

fun <T> select(a: T, b: T) : T = a

interface A
interface B<out T> {
    fun foo(): T
}

interface BB<T1> : B<T1>
interface BBB<T2> : BB<T2>

class C<T3>(val x : T3) : A, BBB<T3> {
    override fun foo() = x
}
class D<T4>(val x: T4) : A, BBB<T4> {
    override fun foo() = x
}

fun test(c: C<String>, d: D<String>): String {
    val intersection = select(c, d)
    return object: BBB<CharSequence>, B<CharSequence> by intersection {}.foo() as String
}

fun box() = test(C("OK"), D("FAIL"))