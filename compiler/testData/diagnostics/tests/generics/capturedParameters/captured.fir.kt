// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class X {
    abstract class Y<T : Any>

    fun <T : Any> foo(y: Y<T>, t: T) {
    }
}


fun testStar(y: X.Y<*>, t: Any) {
    X().<!INAPPLICABLE_CANDIDATE!>foo<!>(y, t)
}

fun testOut(y: X.Y<out Any>, t: Any) {
    X().<!INAPPLICABLE_CANDIDATE!>foo<!>(y, t)
}

fun testIn(y: X.Y<in Any>, t: Any) {
    X().foo(y, t)
}

fun <T : Any> testWithParameter(y: X.Y<T>, t: Any) {
    X().<!INAPPLICABLE_CANDIDATE!>foo<!>(y, t)
}

fun <T : Any> testWithCapturedParameter(y: X.Y<out T>, t: Any) {
    X().<!INAPPLICABLE_CANDIDATE!>foo<!>(y, t)
}
