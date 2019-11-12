class A() {
    class B() : A() {
        fun copy() = <!AMBIGUITY!>B<!>()
    }
}
