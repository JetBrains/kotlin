// LL_FIR_DIVERGENCE
// The compiler doesn't guarantee exhaustiveness in reporting of inheritance cycles, so the compiler and LL FIR results are equally valid.
// LL_FIR_DIVERGENCE
// IGNORE_REVERSED_RESOLVE
// FILE: I.kt

open class I : <!CYCLIC_INHERITANCE_HIERARCHY!>K<!>() {
    fun foo() {}
}

// FILE: J.java

class J extends I {
    void bar() {}
}

// FILE: K.kt

open class K : <!CYCLIC_INHERITANCE_HIERARCHY!>J<!>() {
    fun baz() {}
}
