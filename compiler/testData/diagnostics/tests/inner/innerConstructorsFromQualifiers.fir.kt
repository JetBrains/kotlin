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
            <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
            <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>(1)
        }
    }
}

fun foo() {
    Outer.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
    Outer.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>(1)
}

// FILE: imported.kt
import abc.Outer
import abc.Outer.Inner

fun bar() {
    <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
    <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>(1)

    with(Outer()) {
        Inner()
        Inner(1)
    }
}
