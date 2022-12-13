// JSPECIFY_STATE: warn

// FILE: IgnoreAnnotations.java
import org.jspecify.annotations.*;

@NullMarked
public class IgnoreAnnotations {
    @Nullable public Derived field = null;

    @Nullable
    public Derived foo(Derived x, @NullnessUnspecified Base y) {
        return null;
    }

    public Derived everythingNotNullable(Derived x) { return null; }

    public @Nullable Derived everythingNullable(@Nullable Derived x) { return null; }

    public @NullnessUnspecified Derived everythingUnknown(@NullnessUnspecified Derived x) { return null; }
}

// FILE: Base.java
public class Base {
    void foo() {}
}

// FILE: Derived.java
public class Derived extends Base { }

// FILE: main.kt
fun main(a: IgnoreAnnotations, x: Derived): Unit {
    a.foo(x, null).foo()
    a.foo(null, x).foo()

    a.field.foo()

    a.everythingNotNullable(null).foo()
    a.everythingNotNullable(x).foo()

    a.everythingNullable(null).foo()

    a.everythingUnknown(null).foo()
}
