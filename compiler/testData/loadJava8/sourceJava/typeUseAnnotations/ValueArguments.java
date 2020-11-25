// JAVAC_EXPECTED_FILE

package test;

import java.lang.annotation.*;

class ValueArguments {
    @Target(ElementType.TYPE_USE)
    @interface Anno {
        String value() default "";
    }

    interface G0 { }
    interface G1<T> { }
    interface G2<A, B> { }

    void f0(G1<@Anno G0> p);
    void f1(G1<G1<G1<G1<@Anno G0>>>> p);
    void f2(G1<@Anno String> p);
    void f3(G2<@Anno String, G2<@Anno("abc") Integer, G2<@Anno("abc") G2<Integer, @Anno Integer>, @Anno("abc") Integer>>> p);
}
