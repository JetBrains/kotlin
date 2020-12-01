package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

interface G0 { }
interface G1<T> { }
interface G2<A, B> { }

interface ReturnType {
    // simplpe type arguments
    G1<@A G0> f0();
    G1<G1<G1<G1<@A G0>>>> f1();
    G1<@A String> f2();
    G2<@A String, G2<@A("abc") Integer, G2<@A("abc") G2<Integer, @A Integer>, @A("abc") Integer>>> f3();

    // wildcards
    G1<? extends @A G0> f4 = null;
    G1<G1<G1<G1<? extends @A G0>>>> f5();
    G1<? extends @A String> f6();
    G2<? extends @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? extends @A Integer>, ? extends @A("abc") Integer>>> f7();

    G1<? super @A G0> f8();
    G1<G1<G1<G1<? super @A G0>>>> f9();
    G1<? super @A String> f10 = null;
    G2<? super @A String, G2<? super @A("abc") Integer, G2<? super @A("abc") G2<Integer, ? super @A Integer>, ? super @A("abc") Integer>>> f11();

    G2<? super @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? super @A Integer>, ? extends @A("abc") Integer>>> f12 = null;

    // arrays
    Integer @A [] f13();
    int @A [] f14();
    @A Integer [] f15();
    @A int [] f16();
    @A Integer @A [] f17();
    @A int @A [] f18 = null;

    // multidementional arrays
    Integer @A [] [] f19();
    int @A [] @A [] f20();
    @A Integer [] [] [] f21 = null;
    @A int @A [] @A [] [] @A [] f22();
    @A Integer @A [] [] @A [] [] f23();
    @A int @A [] @A [] f24 = null;
    int [] @A [] f25();
    Object [] @A [] f26();
    @A Object [] [] [] [] @A [] f27();

    // arrays in type arguments
    G1<Integer @A []> f28();
    G2<Integer, int @A []> f29();
    G1<@A Integer []> f30();
    G1<G1<@A int []>> f31();
    G1<G2<G1<@A Integer @A []>, G1<@A int @A []>>> f32();
    G1<@A int @A []> f33();
    G1<Integer [] @A []> f34();
    G2<Integer, int @A [][]> f35 = null;
    G1<@A Integer @A [] []> f36();
    G1<G1<@A int [][][]>> f37();
    G1<G2<G1<@A Integer @A []>, G1<@A int [] [] @A []>>> f38();
    G1<@A int @A [] @A [] []> f39();

    // arrays in wildcard bounds
    G1<? extends Integer @A []> f40();
    G2<? extends Integer, ? super int @A []> f41();
    G1<? super @A Integer []> f42();
    G1<? super G1<? super @A int []>> f43();
    G1<? extends G2<G1<? super @A Integer @A []>, G1<? extends @A int @A []>>> f44();
    G1<? extends G2<? super G1<@A Integer @A []>, G1<? extends @A int @A []>>> f45 = null;
    G1<? super @A int @A []> f46();
    G1<? extends Integer [] @A []> f47();
    G2<? extends Integer, ? super int [] [] @A []> f48 = null;
    G1<? super @A Integer [][][][][]> f49();
    G1<? super G1<? super @A int @A [][]>> f50();
    G1<? extends G2<G1<? super @A Integer [] [] @A []>, G1<? extends @A int @A [] @A [] @A []>>> f51();
    G1<? extends G2<? super G1<@A Integer @A [][][]>, G1<? extends @A int [] @A []>>> f52 = null;
    G1<? super @A int @A [][]> f53();

    class ReturnType2 {
        G1<? extends @A G0> f4 = null;
        G1<? super @A String> f10 = null;
        G2<? super @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? super @A Integer>, ? extends @A("abc") Integer>>> f12 = null;
        @A int @A [] f18 = null;
        @A Integer [] [] [] f21 = null;
        @A int @A [] @A [] f24 = null;
        G2<Integer, int @A [][]> f35 = null;
        G1<? extends G2<? super G1<@A Integer @A []>, G1<? extends @A int @A []>>> f45 = null;
        G2<? extends Integer, ? super int [] [] @A []> f48 = null;
        G1<? extends G2<? super G1<@A Integer @A [][][]>, G1<? extends @A int [] @A []>>> f52 = null;
    }
}
