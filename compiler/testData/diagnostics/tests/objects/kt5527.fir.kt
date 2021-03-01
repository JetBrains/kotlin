// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

object Boo {}
class A {
    object Boo {}
}

fun foo() {
    val i1: Int = <!INITIALIZER_TYPE_MISMATCH!>Boo<!>
    val i2: Int = <!INITIALIZER_TYPE_MISMATCH!>A.Boo<!>
    <!INAPPLICABLE_CANDIDATE!>useInt<!>(Boo)
    <!INAPPLICABLE_CANDIDATE!>useInt<!>(A.Boo)
}
fun bar() {
    val i1: Int = <!INITIALIZER_TYPE_MISMATCH!>Unit<!>
    <!INAPPLICABLE_CANDIDATE!>useInt<!>(Unit)
}

fun useInt(i: Int) = i
