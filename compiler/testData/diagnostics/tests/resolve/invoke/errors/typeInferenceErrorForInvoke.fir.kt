// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T>

operator fun <T> T.invoke(a: A<T>) {}

fun foo(s: String, ai: A<Int>) {
    1(ai)

    <!INAPPLICABLE_CANDIDATE!>s<!>(ai)

    <!INAPPLICABLE_CANDIDATE!>""(ai)<!>
}
