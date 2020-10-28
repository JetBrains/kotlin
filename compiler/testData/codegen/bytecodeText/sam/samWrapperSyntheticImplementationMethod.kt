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

// 2 public final synthetic run\(\)V