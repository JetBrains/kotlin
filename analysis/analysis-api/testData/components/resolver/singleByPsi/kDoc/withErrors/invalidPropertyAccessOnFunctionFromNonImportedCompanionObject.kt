// FILE: A.kt
package bar

class A {
    companion object {
        fun toName(): String = ""
    }
}

// FILE: main.kt
package baz

/**
 * [bar.A.<caret>toName.length]
 */
fun foo() {}
