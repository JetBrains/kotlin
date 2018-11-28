open class A {
    open fun Int.foo(): Int {
        return 0
    }
}

class B: A() {
    <caret>
}