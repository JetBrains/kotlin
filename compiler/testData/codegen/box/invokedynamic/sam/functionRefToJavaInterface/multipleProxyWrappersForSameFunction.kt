// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 4 java/lang/invoke/LambdaMetafactory

// FILE: voidReturnTypeAsObject.kt

var t = "Failed"

fun ok(s: String) { t = s }

fun box(): String {
    Sam(::ok).get("1")
    Sam(::ok).get("2")
    Sam(::ok).get("3")

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
