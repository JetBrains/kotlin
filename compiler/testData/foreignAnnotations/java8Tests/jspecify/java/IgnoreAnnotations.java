import org.jspecify.annotations.*;

@DefaultNonNull
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

class Base {
    void foo() {}
}

class Derived extends Base { }

class Use {
    static void main(IgnoreAnnotations a, Derived x) {
        a.foo(x, null).foo();
        // jspecify_nullness_mismatch
        a.foo(null, x).foo();

        a.field.foo();

        // jspecify_nullness_mismatch
        a.everythingNotNullable(null).foo();
        a.everythingNotNullable(x).foo();

        a.everythingNullable(null).foo();

        a.everythingUnknown(null).foo();
    }
}
