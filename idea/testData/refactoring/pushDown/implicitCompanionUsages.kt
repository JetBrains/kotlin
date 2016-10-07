class X {
    open class <caret>A {
        // INFO: {"checked": "true"}
        fun foo(): Int = bar() + A.bar() + X.A.bar()

        companion object {
            fun bar() = 1
        }
    }
}

class B : X.A