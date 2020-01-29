// FILE: I.kt

open class I : K() {
    fun foo() {}
}

// FILE: J.java

class J extends I {
    void bar() {}
}

// FILE: K.kt

open class K : J() {
    fun baz() {}
}
