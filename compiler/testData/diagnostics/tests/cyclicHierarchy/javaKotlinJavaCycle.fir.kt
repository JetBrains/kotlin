// FILE: A.java

interface A extends C {
    void foo();
}

// FILE: B.kt

interface B : <!EXPOSED_SUPER_INTERFACE!>A<!> {
    fun bar()
}

// FILE: C.java

interface C extends B {
    void baz();
}
