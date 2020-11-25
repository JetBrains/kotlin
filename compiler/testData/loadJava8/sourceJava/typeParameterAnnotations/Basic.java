// JAVAC_EXPECTED_FILE

package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

interface Basic {
    interface G0 { }
    interface G1<T> { }
    interface G2<A, B> { }

    // simplpe type arguments
    G1<@A G0> f0();
    G1<G1<G1<G1<@A G0>>>> f1();
    G1<@A String> f2();
    G2<@A String, G2<@A("abc") Integer, G2<@A("abc") G2<Integer, @A Integer>, @A("abc") Integer>>> f3();

    // wildcards
    G1<? extends @A G0> f4();
    G1<G1<G1<G1<? extends @A G0>>>> f5();
    G1<? extends @A String> f6();
    G2<? extends @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? extends @A Integer>, ? extends @A("abc") Integer>>> f7();

    G1<? super @A G0> f8();
    G1<G1<G1<G1<? super @A G0>>>> f9();
    G1<? super @A String> f10();
    G2<? super @A String, G2<? super @A("abc") Integer, G2<? super @A("abc") G2<Integer, ? super @A Integer>, ? super @A("abc") Integer>>> f11();

    G2<? super @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? super @A Integer>, ? extends @A("abc") Integer>>> f12();
}
