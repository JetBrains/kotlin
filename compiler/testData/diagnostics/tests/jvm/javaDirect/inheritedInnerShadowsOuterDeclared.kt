// RUN_PIPELINE_TILL: FRONTEND

// Per JLS 6.4.1 an *inherited* member type shadows one merely *declared* by a lexically-enclosing
// class. Each sibling declares `make(): Nested`, but `Nested` resolves against a different scope:
//   Case 1 (same-file)  : `FromSameFile extends Base` inherits the same-file sibling `Base.Nested`.
//   Case 2 (cross-file) : `FromCrossFile extends a.Outer` inherits the cross-file `Outer.Nested`.
//   Case 3 (enclosing)  : `FromEnclosing` inherits no `Nested`, so the enclosing `Other.Nested` wins.

// FILE: a/Outer.java
package a;

public class Outer {
    public static class Nested {
        public int fromOuter() { return 1; }
    }
}

// FILE: b/Other.java
package b;

import a.Outer;

public class Other {
    public static class Base {
        public static class Nested {
            public int fromBase() { return 1; }
        }
    }

    public static class Nested {
        public int fromOther() { return 2; }
    }

    public static class FromSameFile extends Base {
        public Nested make() { return null; }
    }

    public static class FromCrossFile extends Outer {
        public Nested make() { return null; }
    }

    public static class FromEnclosing {
        public Nested make() { return null; }
    }
}

// FILE: main.kt
package b

fun case1() = Other.FromSameFile().make().fromBase()

fun case2Outer() = Other.FromCrossFile().make().fromOuter()

fun case2Other() = Other.FromCrossFile().make().<!UNRESOLVED_REFERENCE!>fromOther<!>()

fun case3() = Other.FromEnclosing().make().fromOther()

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
