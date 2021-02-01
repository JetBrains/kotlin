// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: primitiveVsWrapperInSam.kt
var test = 0

fun tf2(k: Int) { test = k * 10 }

fun tf4() = 5678

fun box(): String {
    J.accept42 { k: Int -> test = k }
    if (test != 42) return "Failed 1: test=$test"

    J.accept42(::tf2)
    if (test != 420) return "Failed 2: test=$test"

    val t3 = J.get { 1234 }
    if (t3 != 1234) return "Failed 3: t3=$t3"

    val t4 = J.get(::tf4)
    if (t4 != 5678) return "Failed 4: t4=$t4"

    return "OK"
}

// FILE: J.java
public class J {
    public static void accept42(Sam1 sam) {
        sam.accept(42);
    }

    public static int get(Sam2 sam) {
        return sam.get();
    }
}

// FILE: Sam1.java
public interface Sam1 {
    void accept(Integer x);
}

// FILE: Sam2.java
public interface Sam2 {
    Integer get();
}