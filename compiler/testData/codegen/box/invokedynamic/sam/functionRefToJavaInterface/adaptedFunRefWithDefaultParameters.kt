// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final synthetic class AdaptedFunRefWithDefaultParametersKt\$box\$[0-9]+

// FILE: adaptedFunRefWithDefaultParameters.kt
var ok = "Failed"

fun test(s1: String, s2: String = "K") {
    ok = s1 + s2
}

fun box(): String {
    Sam(::test).foo("O")
    return ok
}

// FILE: Sam.java
public interface Sam {
    void foo(String s);
}
