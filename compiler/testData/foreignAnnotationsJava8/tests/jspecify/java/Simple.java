import org.jspecify.annotations.*;

@DefaultNonNull
public class Simple {
    @Nullable public Derived field = null;

    @Nullable
    public Derived foo(Derived x, @NullnessUnspecified Base y) {
        return null;
    }

    public Derived bar() {
        return null;
    }
}

class Base {}
class Derived extends Base {
    void foo() {}
}

@DefaultNonNull
class Use {
    static public void main(Simple a, Derived x) {
        a.foo(x, null).foo();
        // jspecify_nullness_mismatch
        a.foo(null, x).foo();

        a.bar().foo();

        a.field.foo();
    }
}
