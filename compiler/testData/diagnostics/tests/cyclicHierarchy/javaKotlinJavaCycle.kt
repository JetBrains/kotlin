// FILE: A.java

interface A extends C {
    void foo();
}

// FILE: B.kt

interface B : <!CYCLIC_INHERITANCE_HIERARCHY!>A<!> {
    fun bar()
}

// FILE: C.java

interface C extends B {
    void baz();
}
