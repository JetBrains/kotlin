// DUMP_IR
// LANGUAGE: -NoWhenBranchMatchedExceptionWithMessage
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ JS fails with Error loading module 'kotlin_m1'. Its dependency 'kotlin_m12' was not found
// DONT_TARGET_EXACT_BACKEND: NATIVE
// ^Native fails on Linux with ld.lld: error: duplicate symbol: kfun:box._when_.NoWhenBranchMatchedException.pkg1.S#$clinit_trigger#static(){}
// WASM_IGNORE_FOR: mode=multi-module
// ^KT-88076
// MODULE: m1
// FILE: m1.kt
sealed interface S
class K : S

// MODULE: m12
// FILE: m1.kt
sealed interface S
class K : S
class K2 : S

// MODULE: m2(m1)
// FILE: m2.kt
fun test(s: S): String {
    return when (s) {
        is K -> ""
    }
}

// MODULE: m3(m12, m2)
// FILE: box.kt
fun box(): String {
    try {
        test(K2())
    } catch (e: Exception) {
        val m = e.message
        if (m != null) return "wrong message: $m"
        return "OK"
    }
    return "exception was expected"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, javaFunction, javaType, sealed,
stringLiteral, whenExpression, whenWithSubject */
