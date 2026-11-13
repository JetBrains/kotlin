// callable: /JavaClass.foo
// KT-91552
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_DISTINCT_SYMBOL_POINTERS
// DO_NOT_REQUIRE_DISTINCT_NON_PSI_SYMBOL_POINTERS
// FILE: A.java
public interface A {}

// FILE: B.java
public interface B {}

// FILE: JavaClass.java
public class JavaClass {
    public <T extends A> void foo(T x) {}
    public <T extends B> void foo(T x) {}

    public static <T extends A> void foo(T x, String s) {}
    public static <T extends B> void foo(T x, String s) {}
}

// FILE: main.kt
