// RUN_PIPELINE_TILL: FRONTEND

// Regression test for the level-1 exception documented on
// `resolveInheritedInnerClassToClassId`: `A` implements a generic interface parameterized by
// the unqualified `Nested` — a simple-name reference to `A`'s own nested class, which `A` does
// not declare itself but inherits transitively through `Base` from `Grandparent`. Resolving that
// generic argument happens while `A`'s own `implements` clause (and so its own supertype list)
// is still being computed, so `directSupertypeClassIds(A)` can be cycle-guard-skipped at exactly
// that moment.
//
// `resolveInheritedInnerClassToClassId` instead reads `A`'s own direct supertypes from raw AST
// text (never `directSupertypeClassIds`) for this exact reason, so it never depends on `A`'s own
// guarded supertype computation. Companion test for the qualified-reference shape:
// `qualifiedInheritedNestedClassInOwnImplementsClause.kt`.

// FILE: test/Grandparent.java
package test;

public class Grandparent {
    public static class Nested {
        public int fromGrandparent() { return 1; }
    }
}

// FILE: test/Base.java
package test;

public class Base extends Grandparent {}

// FILE: test/A.java
package test;

import java.util.Comparator;

public class A extends Base implements Comparator<Nested> {
    public int compare(Nested a, Nested b) { return 0; }

    public Nested make() { return null; }
}

// FILE: main.kt
import test.A

fun test(a: A) {
    a.make().fromGrandparent()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, nullableType */
