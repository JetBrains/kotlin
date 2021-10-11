// IGNORE_BACKEND: JVM

// FILE: indySamConversionViaProxyFun.kt.kt
fun test() {
    use(Sam(String?::plus))
    use(Sam(String?::plus))
    use(Sam(String?::plus))
}

fun use(s: Sam) {}

// FILE: Sam.java
public interface Sam {
    String get(String x, Object y);
}
