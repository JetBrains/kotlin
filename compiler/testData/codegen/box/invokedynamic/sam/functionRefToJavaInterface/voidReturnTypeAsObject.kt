// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: voidReturnTypeAsObject.kt
var t = "Failed"

fun ok(s: String) { t = s }

fun box(): String {
    val r = Sam(::ok).get("OK")
    if (r != Unit) {
        return "Failed: $r"
    }
    return t
}

// FILE: Sam.java
public interface Sam {
    Object get(String s);
}
