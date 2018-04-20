// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// FILE: Outer.kt
package abc
class Outer {
    inner class Inner() {
        constructor(x: Int) : this() {}
    }

    companion object {

        fun baz() {
            <!RESOLUTION_TO_CLASSIFIER!>Inner<!>()
            <!RESOLUTION_TO_CLASSIFIER!>Inner<!>(1)
        }
    }
}

fun foo() {
    Outer.<!RESOLUTION_TO_CLASSIFIER!>Inner<!>()
    Outer.<!RESOLUTION_TO_CLASSIFIER!>Inner<!>(1)
}

// FILE: imported.kt
import abc.Outer
import abc.Outer.Inner

fun bar() {
    <!RESOLUTION_TO_CLASSIFIER!>Inner<!>()
    <!RESOLUTION_TO_CLASSIFIER!>Inner<!>(1)

    with(Outer()) {
        Inner()
        Inner(1)
    }
}