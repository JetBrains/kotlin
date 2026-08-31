// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// JVM_EXPOSE_BOXED
// FILE: lib.kt
@JvmInline value class P(val n: Int)

class H(val p: P?)

// MODULE: main(lib)
// FILE: Main.java
public class Main {
    public static H create(P p) {
        return new H(p);
    }

    public static H createNull() {
        return new H(null);
    }
}

// FILE: usage.kt
fun box(): String {
    val fromKotlin = H(P(2))
    if (fromKotlin.p?.n != 2) return "FAIL fromKotlin: " + fromKotlin.p?.n

    val nullFromKotlin = H(null)
    if (nullFromKotlin.p != null) return "FAIL nullFromKotlin: " + nullFromKotlin.p

    val fromJava = Main.create(P(3))
    if (fromJava.p?.n != 3) return "FAIL fromJava: " + fromJava.p?.n

    val nullFromJava = Main.createNull()
    if (nullFromJava.p != null) return "FAIL nullFromJava: " + nullFromJava.p

    return "OK"
}
