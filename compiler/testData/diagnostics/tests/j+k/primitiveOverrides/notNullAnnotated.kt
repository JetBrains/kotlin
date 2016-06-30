// FILE: A.java
import org.jetbrains.annotations.*;

public class A {
    public void foo(int x) {}
    public void bar(@NotNull Double x) {}
}

// FILE: B.java
import org.jetbrains.annotations.*;
public class B extends A {
    public void foo(@NotNull Integer x) {}
    public void bar(double x) {}
}

// FILE: main.kt

fun foo(b: B) {
    // See KT-9182
    b.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1)
    b.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>(2.0)
}
