// FILE: J.java

public class J {
    public static void g(Runnable r) {
        r.run();
    }
}

// FILE: test.kt

fun box(): String {
    var result = "Fail"
    val setter = { result = "OK" }
    1.apply { J.g(setter) }
    return result
}

// 1 INVOKESPECIAL TestKt\$sam\$i\$java_lang_Runnable\$0.<init> \(Lkotlin/jvm/functions/Function0;\)V