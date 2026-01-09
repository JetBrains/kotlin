// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83635

fun foo(x: Long): Int = 1
fun foo(x: Int?): String = ""

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1).<!DEBUG_INFO_MISSING_UNRESOLVED!>length<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral */
