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

// The SAM wrapper constructor is public inside of inline functions.
// It has no other flags. In particular, it is not synthetic.
// 1 access flags 0x0\n\s*<init>\(Lkotlin/jvm/functions/Function0;\)V
// 1 access flags 0x1\n\s*public <init>\(Lkotlin/jvm/functions/Function0;\)V
