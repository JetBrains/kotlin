// FILE: A.java

public interface A {
    default void foo() {}
}

// FILE: B.java

public interface B extends A

// FILE: C.java

public interface C extends A

// FILE: test.kt

class Adapter : B, C

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class D<!>(val adapter: Adapter) : B by adapter, C by adapter
