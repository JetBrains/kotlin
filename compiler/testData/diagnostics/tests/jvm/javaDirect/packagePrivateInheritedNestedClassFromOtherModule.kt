// RUN_PIPELINE_TILL: FRONTEND

// `Widget` extends `a.Base` from another module, so `a.Base` is not part of `main`'s own Java
// source index. `a.Base` declares a package-private nested class `Renderer`, which is not accessible
// from package `b` (JLS 6.6.1), so it must not shadow the same-named top-level `b.Renderer` in `Widget`'s own package.

// MODULE: lib
// FILE: a/Base.java
package a;

public class Base {
    static class Renderer {
        public int fromBase() { return 1; }
    }
}

// MODULE: main(lib)
// FILE: b/Renderer.java
package b;

public class Renderer {
    public int fromOwnPackage() { return 2; }
}

// FILE: b/Widget.java
package b;

public class Widget extends a.Base {
    public Renderer make() { return null; }
}

// FILE: main.kt
import b.Widget

fun test(w: Widget) {
    w.make().fromOwnPackage()
    w.make().<!UNRESOLVED_REFERENCE!>fromBase<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
