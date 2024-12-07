// LANGUAGE: +TypeEnhancementImprovementsInStrictMode

package test;

import org.jetbrains.annotations.*;

abstract class MethodTypeParameterBounds {
    interface I1 {}
    interface I2<T> {}
    interface I3<T> {}
    interface I4<T> {}

    <T extends @NotNull Object> void f1(T x) { }
    <_A, B extends @NotNull Integer> void f2(_A x, B y) { }
    <_A, B extends Object & @Nullable I1> void f3(_A x, B y) { }
    <_A extends @NotNull B, B> void f4(_A x, B y) { }
    <_A, B extends @Nullable _A> void f5(_A x, B y) { }
    <_A extends @Nullable I1> void f6() { }
    abstract <_A, B extends @NotNull _A> void f7(_A x, B y);
    abstract <_A extends @Nullable I1, B, C, D extends @NotNull E, E, F> void f8(_A x1, B x2, C x3, D x4, E x5, F x6);
    <_A extends Object & I2<@Nullable Integer> & @Nullable I3<String>> void f9(_A x) { }
    <_A extends Object & I2<? super @Nullable Integer> & @Nullable I3<? extends String>> void f10(_A x) { }
    <_A extends I4<Integer @NotNull []> & I2<? extends @Nullable Integer @Nullable []> & @Nullable I3<? extends Integer @NotNull []>> void f11(_A x) { }
    <_A extends I4<int @Nullable []> & I2<? extends @Nullable int @Nullable []> & @NotNull I3<? extends int @Nullable []>> void f12(_A x) { }
    <_A extends I4<Integer [] [] @Nullable []> & I2<? extends @Nullable Integer @Nullable [] [] [] []> & @Nullable I3<? extends Integer [] @Nullable []>> void f13(_A x) { }
    abstract <_A extends I4<int @NotNull [][]> & I2<? extends @Nullable int [] [] @Nullable []> & @Nullable I3<? extends int []@Nullable [] []>> void f14(_A x);
    <_A extends Object, B extends I3<@Nullable _A> & @NotNull I2<_A>> void f15(_A x) { }
}
