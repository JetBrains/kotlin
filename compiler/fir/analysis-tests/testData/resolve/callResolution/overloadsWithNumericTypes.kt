// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo1(x: Int): Int = TODO()
fun foo1(x: Long): Long = TODO()

fun foo2(x: List<Int>): Int = TODO()
fun foo2(x: List<Long>): Long = TODO()

fun foo3(x: () -> Int): Int = TODO()
fun foo3(x: () -> Long): Long = TODO()

fun foo4(x: MutableList<Int>): Int = TODO()
fun foo4(x: MutableList<Long>): Long = TODO()

fun main() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>foo1(1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>foo1(1L)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>foo2(listOf(1))<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>foo2(listOf(1L))<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>foo3 { 1 }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>foo3 { 1L }<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: foo4, [/foo4, /foo4]")!><!OVERLOAD_RESOLUTION_AMBIGUITY!>foo4<!>(mutableListOf(1))<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>foo4(mutableListOf(1L))<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral */
