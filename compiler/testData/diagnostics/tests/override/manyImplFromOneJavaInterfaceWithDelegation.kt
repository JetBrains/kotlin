// FIR_IDENTICAL
// FILE: A.java

public interface A {
    default void foo() {}
}

// FILE: B.java

public interface B extends A {}

// FILE: C.java

public interface C extends A {}

// FILE: CK.kt

interface CK : A

// FILE: test.kt

class Adapter : B, C

class D(val adapter: Adapter) : B by adapter, C by adapter
class E(val b: B, val c: C) : B by b, C by c

class AdapterK : B, CK
class F(val adapter: AdapterK) : B by adapter, CK by adapter
