fun main(args: Array<String>) {
    val b: A = C()
    <caret>val a = 1
}

open class A
open class B {
    private fun funInB() {}
}

class C: B()

// INVOCATION_COUNT: 2
// RUNTIME_TYPE: C