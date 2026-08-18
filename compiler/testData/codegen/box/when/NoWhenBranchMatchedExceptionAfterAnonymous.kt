// DONT_TARGET_EXACT_BACKEND: JVM_IR
// ^Implementing sealed interfaces in a different module fails with IncompatibleClassChangeError starting from JVM 17
// WITH_STDLIB
// LANGUAGE: +NoWhenBranchMatchedExceptionWithMessage
// IGNORE_BACKEND: WASM
// ^^^ KT-88074 Package renaming leads to the mismatch with the expected message
// MODULE: m1
// FILE: m1.kt
package pkg1

sealed interface S
class K : S

fun test(s: S): String {
    return when (s) {
        is K -> ""
    }
}

// MODULE: m2(m1)
// FILE: box.kt
package pkg2

import pkg1.*

@Suppress("SEALED_SUPERTYPE_IN_LOCAL_CLASS")
fun box(): String {
    try {
        test(object : S { })
    } catch (e: Exception) {
        val m = e.message!!
        val suffix = when(BACKEND_UNDER_TEST) {
            "WASM_JS", "WASM_WASI" -> "pkg2.<no name provided>"
            "JS_IR", "JS_IR_ES6" -> "[object Object]"
            "NATIVE" -> $$"pkg2.box$1"
            else -> $$"pkg2.BoxKt$box$1"
        }
        if (!m.startsWith("No branch matched for subject: " + suffix)) return "wrong message: $m"
        return "OK"
    }
    return "exception was expected"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, javaFunction, javaType, sealed,
stringLiteral, whenExpression, whenWithSubject */
