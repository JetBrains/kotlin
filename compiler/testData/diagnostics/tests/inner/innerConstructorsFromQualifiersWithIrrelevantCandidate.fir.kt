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
            Inner<!NO_VALUE_FOR_PARAMETER!>()<!>
            Inner(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
            Inner("")
        }
    }
}

fun foo() {
    Outer.Inner<!NO_VALUE_FOR_PARAMETER!>()<!>
    Outer.Inner(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    Outer.Inner("")
}

// FILE: imported.kt
import abc.Outer
import abc.Outer.Inner
import abc.Outer.Companion.Inner

fun bar() {
    Inner<!NO_VALUE_FOR_PARAMETER!>()<!>
    Inner(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    Inner("")

    with(Outer()) {
        Inner()
        Inner(1)
        Inner("")
    }
}
