// JAVAC_EXPECTED_FILE
// LANGUAGE: +TypeEnhancementImprovementsInStrictMode

package test;

import org.jetbrains.annotations.*;

interface G0 { }
interface G1<T> { }
interface G2<A, B> { }

interface ReturnType {
    // simplpe type arguments
    G1<@NotNull G0> f0();
    G1<G1<G1<G1<@NotNull G0>>>> f1();
    G1<@NotNull String> f2();
    G2<@NotNull String, G2<@NotNull Integer, G2<@NotNull G2<Integer, @NotNull Integer>, @NotNull Integer>>> f3();

    // wildcards
    G1<? extends @NotNull G0> f4 = null;
    G1<G1<G1<G1<? extends @NotNull G0>>>> f5();
    G1<? extends @NotNull String> f6();
    G2<? extends @NotNull String, G2<? extends @NotNull Integer, G2<? extends @NotNull G2<Integer, ? extends @NotNull Integer>, ? extends @NotNull Integer>>> f7();

    G1<? super @NotNull G0> f8();
    G1<G1<G1<G1<? super @NotNull G0>>>> f9();
    G1<? super @NotNull String> f10 = null;
    G2<? super @NotNull String, G2<? super @NotNull Integer, G2<? super @NotNull G2<Integer, ? super @NotNull Integer>, ? super @NotNull Integer>>> f11();

    G2<? super @NotNull String, G2<? extends @NotNull Integer, G2<? extends @NotNull G2<Integer, ? super @NotNull Integer>, ? extends @NotNull Integer>>> f12 = null;

    // arrays
    Integer @NotNull [] f13();
    int @NotNull [] f14();
    @NotNull Integer [] f15();
    @NotNull int [] f16();
    @NotNull int [] f161 = null;
    @NotNull Integer @NotNull [] f17();
    @NotNull int @NotNull [] f18 = null;

    // multidementional arrays
    Integer @NotNull [] [] f19();
    int @NotNull [] @NotNull [] f20();
    @NotNull Integer [] [] [] f21 = null;
    @NotNull int @NotNull [] @NotNull [] [] @NotNull [] f22();
    @NotNull Integer @NotNull [] [] @NotNull [] [] f23();
    @NotNull int @NotNull [] @NotNull [] f24 = null;
    int [] @NotNull [] f25();
    Object [] @NotNull [] f26();
    @NotNull Object [] [] [] [] @NotNull [] f27();
    @NotNull Object [] [] [] [] @NotNull [] f271 = null;

    // arrays in type arguments
    G1<Integer @NotNull []> f28();
    G2<Integer, int @NotNull []> f29();
    G1<@NotNull Integer []> f30();
    G1<G1<@NotNull int []>> f31();
    G1<G2<G1<@NotNull Integer @NotNull []>, G1<@NotNull int @NotNull []>>> f32();
    G1<@NotNull int @NotNull []> f33();
    G1<Integer [] @NotNull []> f34();
    G2<Integer, int @NotNull [][]> f35 = null;
    G1<@NotNull Integer @NotNull [] []> f36();
    G1<G1<@NotNull int [][][]>> f37();
    G1<G2<G1<@NotNull Integer @NotNull []>, G1<@NotNull int [] [] @NotNull []>>> f38();
    G1<@NotNull int @NotNull [] @NotNull [] []> f39();

    // arrays in wildcard bounds
    G1<? extends Integer @NotNull []> f40();
    G2<? extends Integer, ? super int @NotNull []> f41();
    G1<? super @NotNull Integer []> f42();
    G1<? super G1<? super @NotNull int []>> f43();
    G1<? extends G2<G1<? super @NotNull Integer @NotNull []>, G1<? extends @NotNull int @NotNull []>>> f44();
    G1<? extends G2<? super G1<@NotNull Integer @NotNull []>, G1<? extends @NotNull int @NotNull []>>> f45 = null;
    G1<? super @NotNull int @NotNull []> f46();
    G1<? extends Integer [] @NotNull []> f47();
    G2<? extends Integer, ? super int [] [] @NotNull []> f48 = null;
    G1<? super @NotNull Integer [][][][][]> f49();
    G1<? super G1<? super @NotNull int @NotNull [][]>> f50();
    G1<? extends G2<G1<? super @NotNull Integer [] [] @NotNull []>, G1<? extends @NotNull int @NotNull [] @NotNull [] @NotNull []>>> f51();
    G1<? extends G2<? super G1<@NotNull Integer @NotNull [][][]>, G1<? extends @NotNull int [] @NotNull []>>> f52 = null;
    G1<? super @NotNull int @NotNull [][]> f53();

    class ReturnType2 {
        G1<? extends @NotNull G0> f4 = null;
        G1<? super @NotNull String> f10 = null;
        G2<? super @NotNull String, G2<? extends @NotNull Integer, G2<? extends @NotNull G2<Integer, ? super @NotNull Integer>, ? extends @NotNull Integer>>> f12 = null;
        @NotNull Integer [] f181 = null;
        @NotNull Integer @NotNull [] f182 = null;
        G1<@NotNull Integer> f183 = null;
        @NotNull Integer [] [] [] f21 = null;
        @NotNull int @NotNull [] @NotNull [] f24 = null;
        G2<Integer, int @NotNull [][]> f35 = null;
        G1<? extends G2<? super G1<@NotNull Integer @NotNull []>, G1<? extends @NotNull int @NotNull []>>> f45 = null;
        G2<? extends Integer, ? super int [] [] @NotNull []> f48 = null;
        G1<? extends G2<? super G1<@NotNull Integer @NotNull [][][]>, G1<? extends @NotNull int [] @NotNull []>>> f52 = null;
    }
}
