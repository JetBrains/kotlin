// !DIAGNOSTICS: -UNUSED_VARIABLE

object Boo {}
class A {
    object Boo {}
}

fun foo() {
    val i1: Int = <!TYPE_MISMATCH!>Boo<!>
    val i2: Int = <!TYPE_MISMATCH!>A.Boo<!>
    useInt(<!TYPE_MISMATCH!>Boo<!>)
    useInt(<!TYPE_MISMATCH!>A.Boo<!>)
}
fun bar() {
    val i1: Int = <!TYPE_MISMATCH!>Unit<!>
    useInt(<!TYPE_MISMATCH!>Unit<!>)
}

fun useInt(i: Int) = i
