// FILE: J.java

class J extends K {
    void foo() {}
}

// FILE: K.kt

class K : <!CYCLIC_INHERITANCE_HIERARCHY!>J<!>() {
    fun bar() {}
}
