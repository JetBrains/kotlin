// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// Regression test for java-direct explicit-import resolution: an explicitly imported *nested*
// class used as a supertype must be resolved with the full package/class split even on the
// reentrance-safe path taken while resolving an inherited inner class.
//
// `Consumer extends Middle` (with `Middle` imported as `a.b.Outer.Middle`) inherits `Inner`.
// Resolving the bare `Inner` re-derives `Middle` through the reentrance-safe explicit-import step,
// which used to split `a.b.Outer.Middle` at the last dot only — as package `a.b.Outer`, class
// `Middle` — and so failed to find `Middle` (and therefore `Inner`). The correct longest-package
// split is package `a.b`, class `Outer.Middle`.

// FILE: a/b/Outer.java
package a.b;

public class Outer {
    public static class Middle {
        public static class Inner {
            public String value() { return "OK"; }
        }
    }
}

// FILE: p/Consumer.java
package p;

import a.b.Outer.Middle;

public class Consumer extends Middle {
    // `Inner` is inherited (not declared) from `Middle`, referenced here by its simple name.
    public Inner make() { return new Inner(); }
}

// FILE: main.kt
fun box(): String {
    return p.Consumer().make().value()
}
