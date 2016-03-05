// WITH_REFLECT
// FILE: J.java

public class J {
}

// FILE: 1.kt

fun box(): String {
    val j = J::class
    if (j.simpleName != "J") return "Fail: ${j.simpleName}"

    return "OK"
}
