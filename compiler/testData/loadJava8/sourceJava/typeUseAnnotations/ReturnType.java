// JAVAC_EXPECTED_FILE

package test;

import java.lang.annotation.*;

interface ReturnType {
    @Target(ElementType.TYPE_USE)
    @interface Anno {
        String value() default "";
    }

    interface G0 { }
    interface G1<T> { }
    interface G2<A, B> { }

    // simplpe type arguments
    //G1<@Anno G0> f0();
    //G1<G1<G1<G1<@Anno G0>>>> f1();
    //G1<@Anno String> f2();
    //G2<@Anno String, G2<@Anno("abc") Integer, G2<@Anno("abc") G2<Integer, @Anno Integer>, @Anno("abc") Integer>>> f3();

    //G1<String @Anno []> f13();

    void f20(@Anno String ... x);
}
