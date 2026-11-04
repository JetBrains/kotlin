// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: Base, Derived
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib reads Java declarations from compiled classes, not from sources.
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
