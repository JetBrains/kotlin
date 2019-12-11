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
            Inner()
            Inner(1)
        }
    }
}

fun foo() {
    Outer.<!UNRESOLVED_REFERENCE!>Inner<!>()
    Outer.<!UNRESOLVED_REFERENCE!>Inner<!>(1)
}

// FILE: imported.kt
import abc.Outer
import abc.Outer.Inner

fun bar() {
    Inner()
    Inner(1)

    with(Outer()) {
        Inner()
        Inner(1)
    }
}