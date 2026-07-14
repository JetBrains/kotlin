// TARGET_BACKEND: JVM_IR

// `a.Base` has a package-private nested `Widget`, which is inherited only inside package `a`.
// `b.Widget` extends `a.Base` from a *different* package, so `a.Base.Widget` is not inherited
// (JLS 8.2) and the unqualified `Widget` in the type of `INSTANCE` must resolve to the top-level
// `b.Widget`, not to the inaccessible inherited nested interface. If it wrongly resolved to
// `a.Base.Widget`, Kotlin would see `INSTANCE` as that interface and `.value()` would be unresolved.

// FILE: a/Base.java
package a;

public class Base {
    interface Widget {}
}

// FILE: b/Widget.java
package b;

public class Widget extends a.Base {
    public static final Widget INSTANCE = new Widget();

    public String value() {
        return "OK";
    }
}

// FILE: main.kt
fun box(): String {
    return b.Widget.INSTANCE.value()
}
