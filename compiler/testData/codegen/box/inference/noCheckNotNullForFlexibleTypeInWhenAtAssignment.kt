// IGNORE_BACKEND_K2: ANY
// TARGET_BACKEND: JVM
// ISSUE: KT-74588

// FILE: MyJava.java
public class MyJava {
    // Nullable
    public static <T extends String> T findViewById(int id) {
        return "OK";
    }
}

// FILE: main.kt
var view: String? = null

// checkNotNull
fun foo(t: String?) {
    view = if (t != null) {
        t
    } else {
        MyJava.findViewById(1000)
    }
}

fun box(): String {
    foo(null)
    if (view != null) return "fail 1"
    foo("OK")
    return view!!
}
