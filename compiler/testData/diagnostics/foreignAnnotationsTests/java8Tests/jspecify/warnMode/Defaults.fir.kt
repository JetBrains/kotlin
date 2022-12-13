// JSPECIFY_STATE: warn

// FILE: Defaults.java
import org.jspecify.annotations.*;

@NullMarked
public class Defaults {
    public Foo defaultField = null;
    @Nullable public Foo field = null;

    public Foo everythingNotNullable(Foo x) { return null; }

    public @Nullable Foo everythingNullable(@Nullable Foo x) { return null; }

    public @NullnessUnspecified Foo everythingUnknown(@NullnessUnspecified Foo x) { return null; }

    public @Nullable Foo mixed(Foo x) { return null; }

    public Foo explicitlyNullnessUnspecified(@NullnessUnspecified Foo x) { return null; }
}

// FILE: Foo.java
public class Foo {
    public Object foo() { return null; }
}

// FILE: main.kt
fun main(a: Defaults, x: Foo): Unit {
    a.everythingNotNullable(null).foo()
    a.everythingNotNullable(x).foo()

    a.everythingNullable(null).foo()

    a.everythingUnknown(null).foo()

    a.mixed(null).foo()
    a.mixed(x).foo()

    a.explicitlyNullnessUnspecified(x).foo()
    a.explicitlyNullnessUnspecified(null).foo()

    a.defaultField.foo()

    a.field.foo()
}
