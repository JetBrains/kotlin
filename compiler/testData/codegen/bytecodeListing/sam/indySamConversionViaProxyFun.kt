// WITH_SIGNATURES
// WITH_STDLIB

// FILE: indySamConversionViaProxyFun.kt.kt
inline fun ifn() {}

fun test() {
    // Proxy functions shouldn't clash
    use(Sam1(String?::plus))
    use(Sam1(String?::plus))
    use(Sam1(String?::plus))

    // Proxy function for inline fun should be non-synthetic
    use(Sam2(::ifn))

    // Proxy function for arrayOf fun should be non-synthetic
    use(Sam3(::intArrayOf))

    // Proxy function for generic fun should have valid generic signature
    use4<Map<String, String>>(::mutableMapOf)
}

fun use(x: Any) {}

fun <U> use4(x: Sam4<U>) {}

// FILE: Sam1.java
public interface Sam1 {
    String get(String x, Object y);
}

// FILE: Sam2.java
public interface Sam2 {
    void run();
}

// FILE: Sam3.java
public interface Sam3 {
    int[] get(int[] s);
}

// FILE: Sam4.java
public interface Sam4<T> {
    T get();
}
