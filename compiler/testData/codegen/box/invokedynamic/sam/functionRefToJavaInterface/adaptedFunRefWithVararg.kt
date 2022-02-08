// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final synthetic class AdaptedFunRefWithVarargKt\$box\$[0-9]+

// FILE: adaptedFunRefWithVararg.kt
var ok = "Failed"

fun test(vararg ss: String) {
    ok = ss[0]
}

fun box(): String {
    Sam(::test).foo("OK")
    return ok
}

// FILE: Sam.java
public interface Sam {
    void foo(String s);
}
