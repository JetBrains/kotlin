// RUN_PIPELINE_TILL: FRONTEND

// `Outer`'s own `implements` clause references the qualified `Outer.Nested` — a nested class
// `Outer` inherits transitively through `Base` from `Grandparent`. The reference is resolved
// while `Outer`'s own supertype list is still being computed, which the resolver must survive
// (see `findInheritedNestedClass`). Simple-name companion:
// `simpleInheritedNestedClassInOwnImplementsClause.kt`.

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

// FILE: test/Outer.java
package test;

import java.util.Comparator;

public class Outer extends Base implements Comparator<Outer.Nested> {
    public int compare(Outer.Nested a, Outer.Nested b) { return 0; }

    public Nested make() { return null; }
}

// FILE: main.kt
import test.Outer

fun test(outer: Outer) {
    outer.make().fromGrandparent()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, nullableType */
