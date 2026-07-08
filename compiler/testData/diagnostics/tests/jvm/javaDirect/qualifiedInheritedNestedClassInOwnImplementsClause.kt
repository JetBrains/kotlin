// RUN_PIPELINE_TILL: FRONTEND

// Regression test for a java-direct resolution-pipeline gap: `Outer` implements a generic
// interface parameterized by `Outer.Nested` — a qualified reference to `Outer`'s own nested
// class `Nested`, which `Outer` does not declare itself but inherits transitively through
// `Base` from `Grandparent`. Resolving that generic argument happens while `Outer`'s own
// `implements` clause (and so its own supertype list) is still being computed, so
// `directSupertypeClassIds(Outer)` can be cycle-guard-skipped at exactly that moment.
//
// Before the fix, `JavaTypeResolver.findInheritedNestedClass` seeded its supertype search
// directly from `directSupertypeClassIds(outerClassId)`, so this guard-skip made the qualified
// `Outer.Nested` reference silently fail to resolve. The fix instead reads `Outer`'s own direct
// supertypes from raw AST text (the same technique already used by
// `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId` for `containingClass`'s own
// supertypes), so it never depends on `Outer`'s own guarded supertype computation.

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
