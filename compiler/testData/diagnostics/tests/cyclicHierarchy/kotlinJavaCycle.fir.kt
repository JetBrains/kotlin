// FILE: J.java

class J extends K {
    void foo() {}
}

// FILE: K.kt

class K : <!EXPOSED_SUPER_CLASS!>J<!>() {
    fun bar() {}
}
