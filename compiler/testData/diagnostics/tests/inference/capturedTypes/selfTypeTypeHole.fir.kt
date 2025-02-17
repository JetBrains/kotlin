// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-62956

abstract class Builder<S, B : Builder<S, B>>(var s: S) {
    fun <T : B> test(x: T): T {
        s = x.s
        return x
    }
}

class BS : Builder<String, BS>("")
class BI : Builder<Int, BI>(1)

fun bar(b: Builder<String, *>, bb: Builder<*, *>) {
    b.test<<!UPPER_BOUND_VIOLATED_CAPTURED_TYPE!>Builder<*, *><!>>(<!MEMBER_PROJECTED_OUT!>bb<!>)
}

fun main() {
    val b = BS()
    bar(b, BI())

    b.s.length
}
