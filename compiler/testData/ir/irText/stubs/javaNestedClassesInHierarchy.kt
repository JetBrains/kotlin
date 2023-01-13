// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// DUMP_EXTERNAL_CLASS: Base, Derived
// FILE: javaNestedClassesInHierarchy.kt

fun test() {}

// FILE: Base.java
public class Base {
    public class BaseInner {}
    public static class BaseNested {}
}

// FILE: Derived.java

public class Derived extends Base {
    public class DerivedInner extends BaseInner {}
    public static class DerivedNested extends BaseNested {}
}
