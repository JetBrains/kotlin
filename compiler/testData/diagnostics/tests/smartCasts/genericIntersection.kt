// FIR_IDENTICAL
// See also KT-7801
class A

fun <T> test(v: T): T {
    val a: T = if (v !is A) v else v
    return a
}
