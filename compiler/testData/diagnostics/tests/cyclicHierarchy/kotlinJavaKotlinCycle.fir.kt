// FILE: I.kt

open class I : <!AMBIGUITY!>K<!>() {
    fun foo() {}
}

// FILE: J.java

class J extends I {
    void bar() {}
}

// FILE: K.kt

open class K : <!AMBIGUITY!>J<!>() {
    fun baz() {}
}
