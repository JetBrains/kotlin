package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

interface G0 { }
interface G1<T> { }
interface G2<A, B> { }

interface ValueArguments {
    // simplpe type arguments
    void f0(G1<@A G0> p);
    void f1(G1<G1<G1<G1<@A G0>>>> p);
    void f2(G1<@A String> p);
    void f3(G2<@A String, G2<@A("abc") Integer, G2<@A("abc") G2<Integer, @A Integer>, @A("abc") Integer>>> p);

    // wildcards
    void f4(G1<? extends @A G0> p);
    void f5(G1<G1<G1<G1<? extends @A G0>>>> p);
    void f6(G1<? extends @A String> p1, G1<G1<G1<G1<? extends @A G0>>>> p2);
    void f7(G2<? extends @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? extends @A Integer>, ? extends @A("abc") Integer>>> p);

    void f8(G1<? super @A G0> p);
    void f9(G1<G1<G1<G1<? super @A G0>>>> p);
    void f10(G1<? super @A String> p);
    void f11(G2<? super @A String, G2<? super @A("abc") Integer, G2<? super @A("abc") G2<Integer, ? super @A Integer>, ? super @A("abc") Integer>>> p);

    void f12(G2<? super @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? super @A Integer>, ? extends @A("abc") Integer>>> p);

    // arrays
    void f13(Integer @A [] p);
    void f14(int @A [] p);
    void f15(@A Integer [] p);
    void f16(@A int [] p);
    void f17(@A Integer @A [] p);
    void f18(@A int @A [] p1, Integer @A [] p2, @A int [] p3);

    // multidementional arrays
    void f19(Integer @A [] [] p);
    void f20(int @A [] @A [] p);
    void f21(@A Integer [] [] [] p);
    void f22(@A int @A [] @A [] [] @A [] p);
    void f23(@A Integer @A [] [] @A [] [] p);
    void f24(@A int @A [] @A [] p);
    void f25(int [] @A [] p);
    void f26(Object [] @A [] p1, int [] @A [] p2, @A int @A [] @A [] [] @A [] p3);
    void f27(@A Object [] [] [] [] @A [] p);

    // arrays in type arguments
    void f28(G1<Integer @A []> p);
    void f29(G2<Integer, int @A []> p);
    void f30(G1<@A Integer []> p);
    void f31(G1<G1<@A int []>> p);
    void f32(G1<G2<G1<@A Integer @A []>, G1<@A int @A []>>> p);
    void f33(G1<@A int @A []> p);
    void f34(G1<Integer [] @A []> p);
    void f35(G2<Integer, int @A [][]> p1, G1<@A Integer @A [] []> p2, G1<G1<@A int []>> p3);
    void f36(G1<@A Integer @A [] []> p);
    void f37(G1<G1<@A int [][][]>> p);
    void f38(G1<G2<G1<@A Integer @A []>, G1<@A int [] [] @A []>>> p);
    void f39(G1<@A int @A [] @A [] []> p);

    // arrays in wildcard bounds
    void f40(G1<? extends Integer @A []> p);
    void f41(G2<? extends Integer, ? super int @A []> p);
    void f42(G1<? super @A Integer []> p);
    void f43(G1<? super G1<? super @A int []>> p);
    void f44(G1<? extends G2<G1<? super @A Integer @A []>, G1<? extends @A int @A []>>> p);
    void f45(G1<? extends G2<? super G1<@A Integer @A []>, G1<? extends @A int @A []>>> p);
    void f46(G1<? super @A int @A []> p);
    void f47(G1<? extends Integer [] @A []> p);
    void f48(G2<? extends Integer, ? super int [] [] @A []> p);
    void f49(G1<? super @A Integer [][][][][]> p1, G1<? super G1<? super @A int @A [][]>> p2, G2<? extends Integer, ? super int [] [] @A []> p3);
    void f50(G1<? super G1<? super @A int @A [][]>> p);
    void f51(G1<? extends G2<G1<? super @A Integer [] [] @A []>, G1<? extends @A int @A [] @A [] @A []>>> p);
    void f52(G1<? extends G2<? super G1<@A Integer @A [][][]>, G1<? extends @A int [] @A []>>> p);
    void f53(G1<? super @A int @A [][]> p);

    void f54(G1<? extends G2<? super G1<@A Integer @A [][][]>, G1<? extends @A int [] @A []>>> p1, G1<@A int @A [] @A [] []> p2, @A Object [] [] [] [] @A [] p3, @A int @A [] p4, G2<? super @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? super @A Integer>, ? extends @A("abc") Integer>>> p5);

    // varargs
    void f55(@A String ... x);
    void f56(String @A ... x);
    void f57(@A String @A ... x);
    void f58(@A int ... x);
    void f59(int @A ... x);
    void f60(@A int @A ... x);

    // varargs + arrays
    void f61(@A String [] ... x);
    void f62(String @A [] ... x);
    void f63(String [] @A ... x);
    void f64(@A String @A [] @A ... x);
    void f65(@A int [] ... x);
    void f66(int @A [] ... x);
    void f67(int [] @A ... x);
    void f68(@A int @A [] @A ... x);

    void f69(@A String [] [] ... x);
    void f70(String [] @A [] ... x);
    void f71(String [] [] [] @A ... x);
    void f72(@A String @A [] [] @A [] @A ... x);
    void f73(@A int [] @A [] ... x);
    void f74(int @A [][][] @A [] ... x);
    void f75(int [] [] [] @A ... x);
    void f76(@A int @A [] [] @A ... x);

    class Test {
        public Test(G2<? super @A String, G2<? extends @A("abc") Integer, G2<? extends @A("abc") G2<Integer, ? super @A Integer>, ? extends @A("abc") Integer>>> p1, Object [] @A [] p2, int [] @A [] p3, @A int @A [] @A [] [] @A [] p4, @A int @A [] [] @A ... p5) {

        }
    }
}
