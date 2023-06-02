// ISSUE: KT-37365

package foo

class Outer<T> {
    inner class Inner {
        fun method() {}
    }
}

fun test() {
    foo.Outer<Int>.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>(42)
    foo.Outer<Int>.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>(42)::<!UNRESOLVED_REFERENCE!>method<!>
}
