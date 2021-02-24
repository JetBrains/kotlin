import org.jspecify.annotations.*;

@DefaultNonNull
class A<T> {
    public void foo(@NullnessUnspecified T t) {}
    public <E> void bar(E e) {}
}

@DefaultNonNull
class B<T> {
    public void foo(T t) {}
    public <E> void bar(E e) {}
}

class Test {}

@DefaultNonNull
public class TypeParameterBounds {
    <T extends Test> void main(A<@Nullable Object> a1, A<Test> a2, B<@Nullable Object> b1, B<Test> b2, T x) {
        // jspecify_nullness_mismatch
        a1.foo(null);
        // jspecify_nullness_mismatch
        a1.<@Nullable T>bar(null);
        a1.<T>bar(x);

        // jspecify_nullness_mismatch
        a2.foo(null);
        // jspecify_nullness_mismatch
        a2.<@Nullable T>bar(null);
        a2.<T>bar(x);

        // jspecify_nullness_mismatch
        b1.foo(null);
        // jspecify_nullness_mismatch
        b1.<@Nullable T>bar(null);
        b1.<T>bar(x);

        // jspecify_nullness_mismatch
        b2.foo(null);
        // jspecify_nullness_mismatch
        b2.<@Nullable T>bar(null);
        b2.<T>bar(x);
    }
}
