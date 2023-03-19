// ISSUE: KT-37365

package foo

class Outer<T> {
    inner class Inner {
        fun method() {}
    }
}

fun test() {
    foo.Outer<Int>.<!RESOLUTION_TO_CLASSIFIER!>Inner<!>(42)
    foo.Outer<Int>.<!RESOLUTION_TO_CLASSIFIER!>Inner<!>(42)::<!UNRESOLVED_REFERENCE!>method<!>
}
