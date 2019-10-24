// PROBLEM: none
// WITH_RUNTIME
fun main() {
    MyEnum.B.foo()
}

enum class MyEnum {
    A {
        override fun foo() {
            println("A")
        }
    },
    B {
        override fun foo() {
            println("B")
            <caret>B.foo()
        }
    };

    abstract fun foo()
}