// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83635

fun foo(x: Long): Int = 1
fun foo(x: Int?): String = ""

fun main() {
    foo(1).length
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral */
