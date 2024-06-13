// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE

// FILE: Base.java
interface Base<T> {}

// FILE: Derived.java
interface Derived<E> extends Base<E> {}

// FILE: Outer.java
import org.checkerframework.checker.nullness.qual.*;

class Outer {
    class A {
        @Nullable Base<@NonNull String> foo() { return null; }
    }

    class B extends A {
        @Override
        Base<String> foo() { return null; }
    }

    class C extends A {
        @Override
        @NonNull Base<String> foo() { return null; }
    }

    class D extends A {
        @Override
        Derived<String> foo() { return null; }
    }

    class E extends A {
        @Override
        @NonNull Derived<String> foo() { return null; }
    }

    class F extends A {
        @Override
        @NonNull Derived<@NonNull String> foo() { return null; }
    }
}
