// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// FILE: Outer.kt
package abc
class Outer {
    inner class Inner() {
        constructor(x: Int) : this() {}
    }

    companion object {
        fun Inner(x: String) {}

        fun baz() {
            // Diagnostic here could be better (why can't I call the constructor above?)
            <!NO_VALUE_FOR_PARAMETER!>Inner()<!>
            Inner(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
            Inner("")
        }
    }
}

fun foo() {
    Outer.<!NO_VALUE_FOR_PARAMETER!>Inner()<!>
    Outer.Inner(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    Outer.Inner("")
}

// FILE: imported.kt
import abc.Outer
import abc.Outer.Inner
import abc.Outer.Companion.Inner

fun bar() {
    <!NO_VALUE_FOR_PARAMETER!>Inner()<!>
    Inner(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    Inner("")

    with(Outer()) {
        Inner()
        Inner(1)
        Inner("")
    }
}
