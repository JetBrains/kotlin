// !CHECK_TYPE
trait A<T>

trait In<in T>

trait Out<out T>

fun <T: Any> doT(t: T?): T = throw Exception("$t")
fun <T: Any> doOut(o: Out<T?>): T { throw Exception("$o") }
fun <T: Any> doIn(i: In<T?>) { throw Exception("$i") }
fun <T: Any> doA(i: A<T?>) { throw Exception("$i") }

fun test(out: Out<Int>, i: In<Int>, inv: A<Int>) {
    // T? >: Int => T = Int
    doT(1)
    val r = doOut(out)
    r checkType { it : _<Int> }

    // T? <: Int => error
    doIn(<!TYPE_MISMATCH!>i<!>)

    // T? >: Int => error
    doA(<!TYPE_MISMATCH!>inv<!>)
}
