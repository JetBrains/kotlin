// FILE: J.java

public class J {
    public static void g(Runnable r) {
        r.run();
    }
}

// FILE: test.kt

fun nonInlineFun() {
    val f = {}
    J.g(f)
}

inline fun inlineFun() {
    val f = {}
    J.g(f)
}

// 1 final class TestKt\$sam\$java_lang_Runnable\$0 implements java/lang/Runnable
// 0 public final class TestKt\$sam\$java_lang_Runnable\$0 implements java/lang/Runnable
// 1 public final class TestKt\$sam\$i\$java_lang_Runnable\$0 implements java/lang/Runnable