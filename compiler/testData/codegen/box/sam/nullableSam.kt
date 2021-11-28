// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Test.java

public class Test {
    public static boolean isNull(Runnable r) {
        if (r == null)
            return true;
        r.run();
        return false;
    }
}

// FILE: test.kt

fun nullableFun(fromNull: Boolean): (() -> Unit)? =
    if (fromNull) null else {{}}

fun box(): String {
    if (!Test.isNull(nullableFun(true))) return "Fail 1"
    if (Test.isNull(nullableFun(false))) return "Fail 2"
    if (!Test.isNull(null)) return "Fail 3"
    if (Test.isNull {}) return "Fail 4"
    return "OK"
}
