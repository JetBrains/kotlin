// FILE: J.java

class J extends K {
    void foo() {}
}

// FILE: K.kt

class K : J() {
    fun bar() {}
}
