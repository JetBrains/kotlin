// MODULE: commonMain
// FILE: main.kt

package a.b.c

fun foo() {}

class Outer {
    fun foo() {}

    class Inner {
        fun foo() {}

        class InnerOfInner {
            fun foo() {}

            private fun check() {
                fun foo() {}
                <expr>a.b.c.Outer.Inner.Companion.foo()</expr>
            }

            companion object {
                fun foo() {}
            }
        }

        companion object {
            fun foo() {}
        }
    }

    companion object {
        fun foo() {}
    }
}