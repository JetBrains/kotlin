// JAVAC_EXPECTED_FILE
// LANGUAGE: +TypeEnhancementImprovementsInStrictMode

package test;

import org.jetbrains.annotations.*;

interface G0 { }
interface G1<T> { }
interface G2<A, B> { }

interface ValueArguments {
    // simplpe type arguments
    void f0(G1<@NotNull G0> p);
    void f1(G1<G1<G1<G1<@NotNull G0>>>> p);
    void f2(G1<@NotNull String> p);
    void f3(G2<@NotNull String, G2<@NotNull Integer, G2<@NotNull G2<Integer, @NotNull Integer>, @NotNull Integer>>> p);

    // wildcards
    void f4(G1<? extends @NotNull G0> p);
    void f5(G1<G1<G1<G1<? extends @NotNull G0>>>> p);
    void f6(G1<? extends @NotNull String> p1, G1<G1<G1<G1<? extends @NotNull G0>>>> p2);
    void f7(G2<? extends @NotNull String, G2<? extends @NotNull Integer, G2<? extends @NotNull G2<Integer, ? extends @NotNull Integer>, ? extends @NotNull Integer>>> p);

    void f8(G1<? super @Nullable G0> p);
    void f9(G1<G1<G1<G1<? super @Nullable G0>>>> p);
    void f10(G1<? super @Nullable String> p);
    void f11(G2<? super @Nullable String, G2<? super @Nullable Integer, G2<? super @Nullable G2<Integer, ? super @NotNull Integer>, ? super @NotNull Integer>>> p);

    void f12(G2<? super @NotNull String, G2<? extends @NotNull Integer, G2<? extends @NotNull G2<Integer, ? super @NotNull Integer>, ? extends @NotNull Integer>>> p);

    // arrays
    void f13(Integer @NotNull [] p);
    void f14(int @NotNull [] p);
    void f15(@NotNull Integer [] p);
    void f16(@Nullable int [] p);
    void f17(@Nullable Integer @Nullable [] p);
    void f18(@Nullable int @Nullable [] p1, Integer @Nullable [] p2, @Nullable int [] p3);

    // multidementional arrays
    void f19(Integer @NotNull [] [] p);
    void f20(int @NotNull [] @NotNull [] p);
    void f21(@NotNull Integer [] [] [] p);
    void f22(@NotNull int @NotNull [] @NotNull [] [] @NotNull [] p);
    void f23(@NotNull Integer @NotNull [] [] @NotNull [] [] p);
    void f24(@NotNull int @NotNull [] @NotNull [] p);
    void f25(int [] @Nullable [] p);
    void f26(Object [] @Nullable [] p1, int [] @Nullable [] p2, @Nullable int @Nullable [] @Nullable [] [] @Nullable [] p3);
    void f27(@Nullable Object [] [] [] [] @Nullable [] p);

    // arrays in type arguments
    void f28(G1<Integer @Nullable []> p);
    void f29(G2<Integer, int @Nullable []> p);
    void f30(G1<@Nullable Integer []> p);
    void f31(G1<G1<@Nullable int []>> p);
    void f32(G1<G2<G1<@NotNull Integer @NotNull []>, G1<@NotNull int @NotNull []>>> p);
    void f33(G1<@NotNull int @NotNull []> p);
    void f34(G1<Integer [] @NotNull []> p);
    void f35(G2<Integer, int @NotNull [][]> p1, G1<@NotNull Integer @NotNull [] []> p2, G1<G1<@NotNull int []>> p3);
    void f36(G1<@NotNull Integer @NotNull [] []> p);
    void f37(G1<G1<@NotNull int [][][]>> p);
    void f38(G1<G2<G1<@NotNull Integer @NotNull []>, G1<@NotNull int [] [] @NotNull []>>> p);
    void f39(G1<@NotNull int @NotNull [] @NotNull [] []> p);

    // arrays in wildcard bounds
    void f40(G1<? extends Integer @NotNull []> p);
    void f41(G2<? extends Integer, ? super int @NotNull []> p);
    void f42(G1<? super @Nullable Integer []> p);
    void f43(G1<? super G1<? super @Nullable int []>> p);
    void f44(G1<? extends G2<G1<? super @Nullable Integer @Nullable []>, G1<? extends @Nullable int @Nullable []>>> p);
    void f45(G1<? extends G2<? super G1<@Nullable Integer @Nullable []>, G1<? extends @NotNull int @NotNull []>>> p);
    void f46(G1<? super @NotNull int @NotNull []> p);
    void f47(G1<? extends Integer [] @NotNull []> p);
    void f48(G2<? extends Integer, ? super int [] [] @NotNull []> p);
    void f49(G1<? super @NotNull Integer [][][][][]> p1, G1<? super G1<? super @NotNull int @NotNull [][]>> p2, G2<? extends Integer, ? super int [] [] @NotNull []> p3);
    void f50(G1<? super G1<? super @NotNull int @NotNull [][]>> p);
    void f51(G1<? extends G2<G1<? super @NotNull Integer [] [] @NotNull []>, G1<? extends @NotNull int @NotNull [] @NotNull [] @NotNull []>>> p);
    void f52(G1<? extends G2<? super G1<@NotNull Integer @NotNull [][][]>, G1<? extends @NotNull int [] @NotNull []>>> p);
    void f53(G1<? super @NotNull int @NotNull [][]> p);

    void f54(G1<? extends G2<? super G1<@NotNull Integer @NotNull [][][]>, G1<? extends @NotNull int [] @NotNull []>>> p1, G1<@NotNull int @NotNull [] @NotNull [] []> p2, @NotNull Object [] [] [] [] @NotNull [] p3, @NotNull int @NotNull [] p4, G2<? super @NotNull String, G2<? extends @NotNull Integer, G2<? extends @NotNull G2<Integer, ? super @NotNull Integer>, ? extends @NotNull Integer>>> p5);

    // varargs
    void f55(@NotNull String ... x);
    void f56(String @NotNull ... x);
    void f57(@NotNull String @NotNull ... x);
    void f58(@NotNull int ... x);
    void f59(int @NotNull ... x);
    void f60(@NotNull int @Nullable ... x);

    // varargs + arrays
    void f61(@Nullable String [] ... x);
    void f62(String @Nullable [] ... x);
    void f63(String [] @Nullable ... x);
    void f64(@NotNull String @NotNull [] @NotNull ... x);
    void f65(@NotNull int [] ... x);
    void f66(int @NotNull [] ... x);
    void f67(int [] @NotNull ... x);
    void f68(@NotNull int @NotNull [] @NotNull ... x);

    void f69(@NotNull String [] [] ... x);
    void f70(String [] @Nullable [] ... x);
    void f71(String [] [] [] @Nullable ... x);
    void f72(@Nullable String @Nullable [] [] @NotNull [] @NotNull ... x);
    void f73(@NotNull int [] @NotNull [] ... x);
    void f74(int @NotNull [][][] @NotNull [] ... x);
    void f75(int [] [] [] @NotNull ... x);
    void f76(@NotNull int @NotNull [] [] @NotNull ... x);

    class Test {
        public Test(G2<? super @NotNull String, G2<? extends @Nullable Integer, G2<? extends @Nullable G2<Integer, ? super @Nullable Integer>, ? extends @NotNull Integer>>> p1, Object [] @NotNull [] p2, int [] @NotNull [] p3, @NotNull int @NotNull [] @NotNull [] [] @NotNull [] p4, @NotNull int @NotNull [] [] @NotNull ... p5) {

        }
    }
}
