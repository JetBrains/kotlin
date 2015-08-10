open class A

class X {
    class <caret>B: A() {
        // INFO: {"checked": "true"}
        fun foo(): Int = bar() + B.bar() + X.B.bar()

        companion object {
            fun bar() = 1
        }
    }
}