// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// JVM_ABI_K1_K2_DIFF: KT-62465

// FILE: test.kt

@JvmOverloads
fun foo(id: Int, vararg pairs: String = emptyArray()): String {
    return "$id: ${java.util.Arrays.toString(pairs)}"
}

@JvmOverloads
fun foo2(id: Int, s: Int = 56, vararg pairs: String): String {
    return "$id, $s: ${java.util.Arrays.toString(pairs)}"
}

fun box(): String {
    if (A.bar1() != "1: []") return "fail 1"
    if (A.bar2() != "2: [OK]") return "fail 2"
    if (A.bar3() != "3, 56: []") return "fail 3"
    if (A.bar4() != "4, 56: [OK]") return "fail 4"
    if (A.bar5() != "5, 1491: [OK]") return "fail 5"

    return "OK"
}

// FILE: A.java

public class A {
    public static String bar1() {
        return TestKt.foo(1);
    }

    public static String bar2() {
        return TestKt.foo(2, "OK");
    }

    public static String bar3() {
        return TestKt.foo2(3);
    }

    public static String bar4() {
        return TestKt.foo2(4, "OK");
    }

    public static String bar5() {
        return TestKt.foo2(5, 1491, "OK");
    }
}
