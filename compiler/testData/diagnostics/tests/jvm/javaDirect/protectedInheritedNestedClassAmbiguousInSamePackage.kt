// RUN_PIPELINE_TILL: FRONTEND

// Counterpart of `protectedInheritedNestedClassInaccessibleFromOtherPackage.kt`: here the
// reference `Derived.Nested` appears in `a.b.SamePkg`, i.e. inside the declaring package `a.b`.
// The protected `OuterClass.Nested` IS accessible there (JLS 6.6.2 — same package), so it and the
// public `MyInterface.Nested` are both in scope and the reference is ambiguous, exactly as javac
// reports it. `make()`'s return type must therefore fail to resolve.

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

// FILE: a/b/SamePkg.java
package a.b;

public class SamePkg {
    public Derived.Nested make() { return null; }
}

// FILE: main.kt
import a.b.SamePkg

fun test(s: SamePkg) {
    s.<!MISSING_DEPENDENCY_CLASS!>make<!>().<!UNRESOLVED_REFERENCE!>fromInterface<!>()
    s.<!MISSING_DEPENDENCY_CLASS!>make<!>().<!UNRESOLVED_REFERENCE!>fromOuter<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
