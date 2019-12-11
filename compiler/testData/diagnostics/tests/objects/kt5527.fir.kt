// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

object Boo {}
class A {
    object Boo {}
}

fun foo() {
    val i1: Int = Boo
    val i2: Int = A.Boo
    <!INAPPLICABLE_CANDIDATE!>useInt<!>(Boo)
    <!INAPPLICABLE_CANDIDATE!>useInt<!>(A.Boo)
}
fun bar() {
    val i1: Int = Unit
    <!INAPPLICABLE_CANDIDATE!>useInt<!>(Unit)
}

fun useInt(i: Int) = i
