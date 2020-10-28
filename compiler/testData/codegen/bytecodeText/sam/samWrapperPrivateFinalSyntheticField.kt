// FILE: J.java

public class J {
    public static void g(Runnable r) {
        r.run();
    }
}

// FILE: test.kt

fun f() {
    val r: () -> Unit = {}
    J.g(r)
}

// 1 private final synthetic Lkotlin/jvm/functions/Function0; function