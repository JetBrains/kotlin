// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// FILE: J.java
public class J {
    public static String s() {
        return "K";
    }
}

// FILE: test.kt
var storage = ""

context(c1: Int, c2: String)
var prop: String
    get() = storage + c2 + c1
    set(value) {
        storage = value
    }

fun box(): String = context(1, J.s()) {
    class B {
        var y by ::prop
    }
    val b = B()
    b.y = "O"
    if (storage != "O") return@context "FAIL 1: $storage"
    if (b.y != "OK1") return@context "FAIL 2: ${b.y}"
    "OK"
}
