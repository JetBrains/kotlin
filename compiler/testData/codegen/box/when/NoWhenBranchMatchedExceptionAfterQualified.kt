// WITH_STDLIB
// LANGUAGE: +NoWhenBranchMatchedExceptionWithMessage
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ qualifiedName returns null on JS
// DONT_TARGET_EXACT_BACKEND: NATIVE
// ^Native fails on Linux with ld.lld: error: duplicate symbol: kfun:box._when_.NoWhenBranchMatchedException.pkg1.S#$clinit_trigger#static(){}
// MODULE: m1
// FILE: m1.kt
package pkg1

sealed interface S
class K : S

// MODULE: m12
// FILE: m1.kt
package pkg1

sealed interface S
class K : S
class K2 : S

// MODULE: m2(m1)
// FILE: m2.kt
package pkg1

fun test(s: S): String {
    return when (s) {
        is K -> ""
    }
}

// MODULE: m3(m12, m2)
// FILE: box.kt
package pkg1

fun box(): String {
    try {
        test(K2())
    } catch (e: Exception) {
        val m = e.message!!
        if (!m.startsWith("No branch matched for subject: pkg1.K2")) return "wrong message: $m"
        return "OK"
    }
    return "exception was expected"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, javaFunction, javaType, sealed,
stringLiteral, whenExpression, whenWithSubject */
