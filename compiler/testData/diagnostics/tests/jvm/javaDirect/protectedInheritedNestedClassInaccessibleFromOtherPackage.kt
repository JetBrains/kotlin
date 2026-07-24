// RUN_PIPELINE_TILL: FRONTEND

// `Derived` inherits two nested classes named `Nested`: `OuterClass.Nested` (protected) and
// `MyInterface.Nested` (public). The reference `a.b.Derived.Nested` appears in `c.d.OtherClass`,
// which is neither in package `a.b` nor a subclass of `OuterClass`, so the protected
// `OuterClass.Nested` is NOT accessible there (JLS 6.6.2). The reference is therefore NOT
// ambiguous and resolves to the public `a.b.MyInterface.Nested` — exactly as javac resolves it.
// So `fromInterface()` resolves and `fromOuter()` does not.

// FILE: a/b/MyInterface.java
package a.b;

public interface MyInterface {
    public class Nested {
        public int fromInterface() { return 1; }
    }
}

// FILE: a/b/OuterClass.java
package a.b;

public class OuterClass {
    protected static class Nested {
        public int fromOuter() { return 2; }
    }
}

// FILE: a/b/Derived.java
package a.b;

public class Derived extends OuterClass implements MyInterface {
}

// FILE: c/d/OtherClass.java
package c.d;

public class OtherClass {
    public a.b.Derived.Nested make() { return null; }
}

// FILE: main.kt
import c.d.OtherClass

fun test(o: OtherClass) {
    o.make().fromInterface()
    o.make().<!UNRESOLVED_REFERENCE!>fromOuter<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
