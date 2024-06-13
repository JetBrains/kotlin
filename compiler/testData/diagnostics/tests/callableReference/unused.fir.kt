// DIAGNOSTICS: +UNUSED_EXPRESSION
fun foo() {

}

class A {
    fun foo() {

    }

    class B
}

fun A.bar() {

}

fun test() {
    ::foo
    ::A
    A::B
    A::foo
    A::bar
}
