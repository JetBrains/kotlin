// FILE: Outer.java

import org.checkerframework.checker.nullness.qual.*;

interface X<T> {}
interface Y<T> {}

class Outer {
    class A {
        <K, V> V foo(K x) { return null; }

        <T> X<T> bar(Y<T> x) { return null; }
    }

    class B extends A {
        // OK, non-platform types
        @Override
        @NonNull
        <T1, T2> T2 foo(@Nullable T1 x) { return null; }

        // Parameter type is fully non-flexible (OK)
        // Return type is `X<R!>?`.
        // The reason is that we do not treat it as equal to return type of A.bar because they are base on different type parameters,
        // so type enhancing happens only for outermost type.
        // TODO: We should properly compare equality with specific local equality axioms (as when calculating overriden descriptors)
        @Override
        @Nullable
        <R> X<@Nullable R> bar(@NonNull Y<@NonNull R> x) { return null; }
    }

    class C extends B {
        // OK, non-platform types
        @Override
        <I, J> J foo(I x) { return null; }

        // Parameter type is fully non-flexible (OK)
        // Return type is `X<R!>?`, same is in B
        @Override
        <E> X<E> bar(Y<E> x) { return null; }
    }

    class D extends C {
        // Return type is not-nullable, covariantly overridden, OK
        // Parameter type is flexible, because of conflict with supertype, OK
        @Override
        @NonNull
        <U, W> W foo(@Nullable U x) { return null; }


        @Override
        @NonNull
        <F> X<@NonNull F> bar(@Nullable Y<@Nullable F> x) { return null; }
    }
}
