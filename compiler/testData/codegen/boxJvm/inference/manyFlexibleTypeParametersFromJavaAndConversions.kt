// TARGET_BACKEND: JVM

// FILE: Combiner.java

public class Combiner {
    public static <T1, T2, T3, T4, T5, R> void combine(
        Inv<? extends T1> source1, Inv<? extends T2> source2,
        Inv<? extends T3> source3, Inv<? extends T4> source4,
        Inv<? extends T5> source5,
        Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> combiner
    ) {
        return;
    }
}

// FILE: Function5.java

public interface Function5<T1, T2, T3, T4, T5, R> {
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
}

// FILE: Test.kt

fun <T1, T2, T3, T4, T5, R> kCombine(
    s1: Inv<out T1>, s2: Inv<out T2>, s3: Inv<out T3>, s4: Inv<out T4>, s5: Inv<out T5>,
    f: Function5<in T1, in T2, in T3, in T4, in T5, out R>
) {
}

data class Quantiple<out T1, out T2, out T3, out T4, out T5>(
    val first: T1,
    val second: T2,
    val third: T3,
    val fourth: T4,
    val fifth: T5
)

fun <K1, K2, K3, K4, K5> materialize(): (K1, K2, K3, K4, K5) -> Quantiple<K1, K2, K3, K4, K5> = { _, _, _, _, _ -> TODO() }

class Inv<T>

fun <P1, P2, P3, P4, P5> test(
    p1: Inv<out P1>, p2: Inv<out P2>, p3: Inv<out P3>, p4: Inv<out P4>, p5: Inv<out P5>
) {
    Combiner.combine(
        p1, p2, p3, p4, p5,
        materialize()
    )

    Combiner.combine(
        p1, p2, p3, p4, p5,
        ::Quantiple
    )

    kCombine(
        p1, p2, p3, p4, p5,
        materialize()
    )
}

fun box(): String {
    test(Inv<Int>(), Inv<Byte>(), Inv<Short>(), Inv<Long>(), Inv<String>())
    return "OK"
}