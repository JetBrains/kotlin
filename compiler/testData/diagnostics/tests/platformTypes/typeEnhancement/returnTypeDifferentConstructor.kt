// FILE: Outer.java

import org.jetbrains.annotations.*;

interface Base<T> {}
interface Derived<E> extends Base<E> {}

class Outer {
    class A {
        @Nullable Base<@NotNull String> foo() { return null; }
    }

    class B extends A {
        @Override
        Base<String> foo() {}
    }

    class C extends A {
        @Override
        @NotNull Base<String> foo() {}
    }

    class D extends A {
        @Override
        Derived<String> foo() {}
    }

    class E extends A {
        @Override
        @NotNull Derived<String> foo() {}
    }

    class F extends A {
        @Override
        @NotNull Derived<@NotNull String> foo() {}
    }
}
