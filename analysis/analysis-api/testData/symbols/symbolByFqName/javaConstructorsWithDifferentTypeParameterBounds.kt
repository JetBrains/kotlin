// constructor: /JavaClass.init
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
public class JavaClass<E extends CharSequence> {
    public <T extends A> JavaClass(T x) {}
    public <T extends B> JavaClass(T x) {}
}

// FILE: main.kt
