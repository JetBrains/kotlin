// JAVAC_EXPECTED_FILE
// FILE: AnnotatedTypeArguments.java

import org.checkerframework.checker.nullness.qual.*;

interface P<X, Y> {}
interface L<T> {}
interface S<T> {}

class AnnotatedTypeArguments {

    class A {
        L<P<L<String>, S<?>>> foo(L<P<L<String>, S<?>>> x) {return null;}
    }

    class B extends A {
        // some complicated type tree
        // return type and argument's type differ only by nullability of outermost type
        @Nullable
        L<P<@NonNull L<@Nullable String>, @NonNull S<?>>> foo(@NonNull L<P<@NonNull L<@Nullable String>, @NonNull S<?>>> x) {return null;}
    }

    class C extends B {
        // signature should be the same as in A
        L<P<L<String>, S<?>>> foo(L<P<L<String>, S<?>>> x) {return null;}
    }

    class D1 extends C {
        // signature should be the same as in A, but annotated String-type should be platform
        L<P<L<@NonNull String>, S<?>>> foo(L<P<L<@NonNull String>, S<?>>> x) {return null;}
    }

    class D2 extends C {
        // return type refined to not-nullable
        // argument type here same as in A except outermost type (it becomes flexible because of conflict)
        @NonNull
        L<P<L<String>, S<?>>> foo(@Nullable L<P<L<String>, S<?>>> x) {return null;}
    }
}
