// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

object Boo {}
class A {
    object Boo {}
}

fun foo() {
    val i1: Int = Boo
    val i2: Int = A.Boo
    useInt(<!ARGUMENT_TYPE_MISMATCH!>Boo<!>)
    useInt(A.<!ARGUMENT_TYPE_MISMATCH!>Boo<!>)
}
fun bar() {
    val i1: Int = Unit
    useInt(<!ARGUMENT_TYPE_MISMATCH!>Unit<!>)
}

fun useInt(i: Int) = i
