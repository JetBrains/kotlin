// DUMP_IR
// MODULE: m1
// FILE: m1.kt
sealed interface S
class K : S

fun test(s: S): String {
    return when (s) {
        is K -> ""
    }
}


// MODULE: m2(m1)
// FILE: box.kt
@Suppress("SEALED_INHERITOR_IN_DIFFERENT_MODULE")
class K2 : S

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
