// PROBLEM: none

class A {
    fun put(x: Int, y: String) {
    }
}

fun foo() {
    A().put<caret>(1, "foo")
}