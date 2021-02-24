import org.jspecify.annotations.*;

@DefaultNonNull
public class Defaults {
    public Foo defaultField = null;
    @Nullable public Foo field = null;

    public Foo everythingNotNullable(Foo x) { return null; }

    public @Nullable Foo everythingNullable(@Nullable Foo x) { return null; }

    public @NullnessUnspecified Foo everythingUnknown(@NullnessUnspecified Foo x) { return null; }

    public @Nullable Foo mixed(Foo x) { return null; }

    public Foo explicitlyNullnessUnspecified(@NullnessUnspecified Foo x) { return null; }
}

class Foo {
    public Object foo() { return null; }
}

class Use {
    static void main(Defaults a, Foo x) {
        // jspecify_nullness_mismatch
        a.everythingNotNullable(null).foo();
        a.everythingNotNullable(x).foo();

        a.everythingNullable(null).foo();

        a.everythingUnknown(null).foo();

        // jspecify_nullness_mismatch
        a.mixed(null).foo();
        a.mixed(x).foo();

        a.explicitlyNullnessUnspecified(x).foo();
        a.explicitlyNullnessUnspecified(null).foo();

        a.defaultField.foo();

        a.field.foo();
    }
}
