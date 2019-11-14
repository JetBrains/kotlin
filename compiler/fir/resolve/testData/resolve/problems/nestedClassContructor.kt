/*
 * We see same constructor of `B` in two scopes
 */

class A() {
    class B() : A() {
        fun copy() = <!AMBIGUITY!>B<!>()
    }
}
