// !LANGUAGE: -SingleUnderscoreForParameterName

data class A(val x: Int, val y: Int)

fun foo(a: Array<A>) {
    val (<!UNSUPPORTED_FEATURE!>_<!>, y) = A(1, 2)
    y.hashCode()

    val q1: (Int, String) -> Unit = {
        <!UNSUPPORTED_FEATURE!>_<!>, s -> s.hashCode()
    }
    q1(1, "")

    val q2: (Int, String) -> Unit = fun(<!UNSUPPORTED_FEATURE!>_<!>: Int, s: String) {
        s.hashCode()
    }
    q2(1, "")

    val q3: (A) -> Unit = {
        (<!UNSUPPORTED_FEATURE!>_<!>, y) -> y.hashCode()
    }
    q3(A(2, 3))

    for ((<!UNSUPPORTED_FEATURE!>_<!>, z) in a) {
        z.hashCode()
    }
}
