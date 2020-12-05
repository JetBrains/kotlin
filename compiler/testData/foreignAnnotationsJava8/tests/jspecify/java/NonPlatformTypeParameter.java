import org.jspecify.annotations.*;

public class NonPlatformTypeParameter<T extends @Nullable Object> {
    public void foo(T t) {}
    public <E extends @Nullable Object> void bar(E e) {}
}

class Test {}

@DefaultNonNull
class Use {
    public <T extends Test> void main(NonPlatformTypeParameter<@Nullable Object> a1, NonPlatformTypeParameter<Test> a2, T x) {
        a1.foo(null);
        a1.<@Nullable Test>bar(null);
        // jspecify_nullness_mismatch
        a1.<T>bar(null);
        a1.<T>bar(x);

        // jspecify_nullness_mismatch
        a2.foo(null);
        a2.<@Nullable Test>bar(null);
        // jspecify_nullness_mismatch
        a2.<T>bar(null);
        a2.<T>bar(x);
    }
}
