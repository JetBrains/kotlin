// JAVAC_EXPECTED_FILE

package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

abstract class MethodTypeParameterBounds {
    interface I1 {}
    interface I2<T> {}
    interface I3<T> {}
    interface I4<T> {}

    <T extends @A Object> void f1(T x) { }
    <_A, B extends @A Integer> void f2(_A x, B y) { }
    <_A, B extends Object & @A I1> void f3(_A x, B y) { }
    <_A extends @A B, B> void f4(_A x, B y) { }
    <_A, B extends @A _A> void f5(_A x, B y) { }
    <_A extends @A I1> void f6() { }
    abstract <_A, B extends @A _A> void f7(_A x, B y);
    abstract <_A extends @A I1, B, C, D extends @A E, E, F> void f8(_A x1, B x2, C x3, D x4, E x5, F x6);
    <_A extends Object & I2<@A Integer> & @A I3<String>> void f9(_A x) { }
    <_A extends Object & I2<? super @A Integer> & @A I3<? extends String>> void f10(_A x) { }
    <_A extends I4<Integer @A []> & I2<? extends @A Integer @A []> & @A I3<? extends Integer @A []>> void f11(_A x) { }
    <_A extends I4<int @A []> & I2<? extends @A int @A []> & @A I3<? extends int @A []>> void f12(_A x) { }
    <_A extends I4<Integer [] [] @A []> & I2<? extends @A Integer @A [] [] [] []> & @A I3<? extends Integer [] @A []>> void f13(_A x) { }
    abstract <_A extends I4<int @A [][]> & I2<? extends @A int [] [] @A []> & @A I3<? extends int []@A [] []>> void f14(_A x);
    <_A extends Object, B extends I3<@A A> & @A I2<A>> void f15(_A x) { }
}
