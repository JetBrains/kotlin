// FIR_IDENTICAL
// JDK_KIND: FULL_JDK_11

// In this test, D depends on C (which requires B non-transitively) and on B; also B transitively requires A.
// We check that if we depend on both C and B, we still transitively depend on A (via B).
// This is a check against an incorrectly implemented DFS which, upon entering C, would write off B as "visited"
// and not enter it later even though we explicitly depend on it in D's module-info

// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports a;
}

// FILE: a/A.java
package a;

public class A {}

// MODULE: moduleB(moduleA)
// FILE: module-info.java
module moduleB {
    requires transitive moduleA;
}

// MODULE: moduleC(moduleA, moduleB)
// FILE: module-info.java
module moduleC {
    requires moduleB;
}

// MODULE: moduleD(moduleC, moduleB, moduleA)
// FILE: module-info.java
module moduleD {
    requires moduleC;
    requires moduleB;

    requires kotlin.stdlib;
}

// FILE: usage.kt
import a.A

fun usage(): String {
    return A().toString()
}
