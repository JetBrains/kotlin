// WITH_STDLIB
// LANGUAGE: +NoWhenBranchMatchedExceptionWithMessage
// TARGET_BACKEND: JS_IR
// ^ qualifiedName returns null on JS
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

@Suppress("SEALED_INHERITOR_IN_DIFFERENT_MODULE", "SEALED_INHERITOR_IN_DIFFERENT_PACKAGE")
class K2 : S

fun box(): String {
    try {
        test(K2())
    } catch (e: Exception) {
        val m = e.message!!
        if (!m.startsWith("No branch matched for subject: K2")) return "wrong message: $m"
        return "OK"
    }
    return "exception was expected"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, javaFunction, javaType, sealed,
stringLiteral, whenExpression, whenWithSubject */
