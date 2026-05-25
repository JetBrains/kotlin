// RUN_PIPELINE_TILL: FRONTEND

// `A`'s own `implements` clause references the unqualified `Nested` — a nested class `A` inherits
// transitively through `Base` from `Grandparent`. The reference is resolved while `A`'s own
// supertype list is still being computed, which the resolver must survive (see
// `resolveInheritedInnerClassToClassId`). Qualified-reference companion:
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
