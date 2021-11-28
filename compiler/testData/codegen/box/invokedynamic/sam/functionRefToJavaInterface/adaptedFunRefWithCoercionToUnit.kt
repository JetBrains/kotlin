// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final synthetic class AdaptedFunRefWithCoercionToUnitKt\$box\$[0-9]+

// FILE: adaptedFunRefWithCoercionToUnit.kt
var ok = "Failed"

fun test(s: String): Int {
    ok = s
    return 42
}

fun box(): String {
    Sam(::test).foo("OK")
    return ok
}

// FILE: Sam.java
public interface Sam {
    void foo(String s);
}
