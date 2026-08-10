// TARGET_BACKEND: JVM_IR

// `a.Base` declares a package-private nested `Renderer`, which is NOT accessible from package
// `b` (JLS 6.6.1), so it must not shadow the same-named top-level `b.Renderer` in `Widget`'s own
// package: `make()` returns `b.Renderer`, exactly as javac resolves the simple name.

// FILE: a/Base.java
package a;

public class Base {
    static class Renderer {
        public String value() { return "FAIL: a.Base.Renderer"; }
    }
}

// FILE: b/Renderer.java
package b;

public class Renderer {
    public String value() { return "OK"; }
}

// FILE: b/Widget.java
package b;

public class Widget extends a.Base {
    public Renderer make() { return new Renderer(); }
}

// FILE: main.kt
fun box(): String = b.Widget().make().value()
