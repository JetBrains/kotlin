// JAVAC_EXPECTED_FILE
// FILE: Outer.java

import org.checkerframework.checker.nullness.qual.*;

interface Base<T> {}
interface Derived<E> extends Base<E> {}

class Outer {
    class A {
        @Nullable Base<@NonNull String> foo() { return null; }
    }

    class B extends A {
        @Override
        Base<String> foo() {}
    }

    class C extends A {
        @Override
        @NonNull Base<String> foo() {}
    }

    class D extends A {
        @Override
        Derived<String> foo() {}
    }

    class E extends A {
        @Override
        @NonNull Derived<String> foo() {}
    }

    class F extends A {
        @Override
        @NonNull Derived<@NonNull String> foo() {}
    }
}
