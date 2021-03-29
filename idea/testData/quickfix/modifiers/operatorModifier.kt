// "Add 'operator' modifier" "true"
class A {
    fun plus(a: A): A = A()
}

fun foo() {
    A() <caret>+ A()
}

/* IGNORE_FIR */