package stopInWrongClass

class A {
    fun test() {
        // Breakpoint 1
        foo()
    }
}

fun foo() {
}