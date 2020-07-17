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
        a.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}