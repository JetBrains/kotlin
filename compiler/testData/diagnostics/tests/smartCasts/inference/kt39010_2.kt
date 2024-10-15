// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
open class A<E> {
}

class B : A<String>() {
    fun foo() {}
}

interface KI {
    val a: A<*>
}

fun KI.bar() {
    if (a is B) {
        <!SMARTCAST_IMPOSSIBLE!>a<!>.foo()
    }
}