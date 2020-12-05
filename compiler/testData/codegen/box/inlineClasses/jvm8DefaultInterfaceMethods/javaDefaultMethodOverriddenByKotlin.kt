// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: JVM
// ^ KT-43698, fixed in JVM_IR
// JVM_TARGET: 1.8
// FILE: javaDefaultMethod.kt
interface K2 : J {
    override fun get2() = "Kotlin"
}

inline class K(val k: String) : K2 {
    override fun get2() = k
}

fun box(): String {
    val k = K("K")

    val test1 = k.get1() + k.get2()
    if (test1 != "OK") throw AssertionError("test1: $test1")

    val j: J = k
    val test2 = j.get1() + j.get2()
    if (test2 != "OK") throw AssertionError("test2: $test2")

    val test3 = JT.test(k)
    if (test3 != "OK") throw AssertionError("test3: $test3")

    val k2: K2 = k
    val test4 = k2.get1() + k2.get2()
    if (test4 != "OK") throw AssertionError("test4: $test4")

    val test5 = JT.test(k2)
    if (test5 != "OK") throw AssertionError("test5: $test5")

    return "OK"
}

// FILE: J.java
public interface J {
    default String get1() { return "O"; }
    default String get2() { return "Java"; }
}

// FILE: JT.java
public class JT {
    public static String test(J j) {
        return j.get1() + j.get2();
    }
}