// "Add 'infix' modifier" "true"
class A {
    fun xyzzy(i: Int) {}
}

fun foo() {
    A() xyz<caret>zy 5
}
/* IGNORE_FIR */