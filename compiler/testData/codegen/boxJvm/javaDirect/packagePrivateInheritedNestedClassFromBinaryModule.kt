// TARGET_BACKEND: JVM_IR

// Same shape as `box/javaDirect/packagePrivateInheritedNestedClassNotVisibleAcrossPackages.kt`,
// but `a.Base` lives in a dependency module, so `main` reads it as a *binary* Java class.

// MODULE: lib
// FILE: a/Base.java
package a;

public class Base {
    static class Renderer {
        public String value() { return "FAIL: a.Base.Renderer"; }
    }
}

// MODULE: main(lib)
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
