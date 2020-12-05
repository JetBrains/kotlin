import org.jspecify.annotations.*;

@DefaultNonNull
public class NullnessUnspecifiedTypeParameter<T> {
    public void foo(T t) {}

    public void bar(Test s, T t) {} // t should not become not nullable
}

class Test {}

class Use {
    void main(NullnessUnspecifiedTypeParameter<Object> a1, NullnessUnspecifiedTypeParameter<@Nullable Object> a2, Test x) {
        a1.foo(null);
        a1.foo(1);

        a2.foo(null);
        a2.foo(1);

        // jspecify_nullness_mismatch
        a1.bar(null, null);
        a1.bar(x, null);
        a1.bar(x, 1);

        // jspecify_nullness_mismatch
        a2.bar(null, null);
        a2.bar(x, null);
        a2.bar(x, 1);
    }
}