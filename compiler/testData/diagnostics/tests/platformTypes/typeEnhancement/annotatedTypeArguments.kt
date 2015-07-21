// FILE: AnnotatedTypeArguments.java

import org.jetbrains.annotations.*;

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
        L<P<@NotNull L<@Nullable String>, @NotNull S<?>>> foo(@NotNull L<P<@NotNull L<@Nullable String>, @NotNull S<?>>> x) {return null;}
    }

    class C extends B {
        // signature should be the same as in A
        L<P<L<String>, S<?>>> foo(L<P<L<String>, S<?>>> x) {return null;}
    }

    class D1 extends C {
        // signature should be the same as in A, but annotated String-type should be platform
        L<P<L<@NotNull String>, S<?>>> foo(L<P<L<@NotNull String>, S<?>>> x) {return null;}
    }

    class D2 extends C {
        // return type refined to not-nullable
        // argument type here same as in A except outermost type (it becomes flexible because of conflict)
        @NotNull
        L<P<L<String>, S<?>>> foo(@Nullable L<P<L<String>, S<?>>> x) {return null;}
    }
}
