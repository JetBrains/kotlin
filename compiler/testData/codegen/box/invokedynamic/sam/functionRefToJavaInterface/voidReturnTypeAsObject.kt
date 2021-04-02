// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final synthetic class VoidReturnTypeAsObjectKt\$box\$r\$[0-9]+

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
